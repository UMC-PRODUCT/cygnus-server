import { createHash } from "node:crypto";
import {
  BatchGetCommand,
  GetCommand,
  QueryCommand,
  TransactWriteCommand,
  UpdateCommand
} from "@aws-sdk/lib-dynamodb";
import { config } from "../shared/config.js";
import { dynamodb } from "../shared/aws.js";
import { partition } from "../shared/sqs.js";
import type { Installation, RegisterInstallation } from "../domain/installation.js";

const ACTIVE = "ACTIVE";
const VALIDATION_KEY = "ACTIVE";

export class InstallationRepository {
  async register(command: RegisterInstallation): Promise<void> {
    const tokenHash = hashToken(command.fcmToken);
    const owner = await this.getOwner(tokenHash);
    const current = await this.get(command.installationId);
    const [previousOwnerInstallation, currentTokenOwner] = await Promise.all([
      owner === undefined || owner.installationId === command.installationId
        ? Promise.resolve(undefined)
        : this.get(owner.installationId),
      current === undefined || current.tokenHash === tokenHash
        ? Promise.resolve(undefined)
        : this.getOwner(current.tokenHash)
    ]);

    const transactionItems: ConstructorParameters<typeof TransactWriteCommand>[0]["TransactItems"] = [];

    if (owner !== undefined
      && owner.installationId !== command.installationId
      && previousOwnerInstallation?.status === ACTIVE
      && previousOwnerInstallation.tokenHash === tokenHash) {
      transactionItems.push({
        Update: {
          TableName: config.installationsTable,
          Key: { installationId: owner.installationId },
          UpdateExpression: "SET #status = :inactive, deactivatedAt = :now REMOVE memberStatusKey, validationStatusKey",
          ConditionExpression: "tokenHash = :tokenHash AND #status = :active",
          ExpressionAttributeNames: { "#status": "status" },
          ExpressionAttributeValues: {
            ":inactive": "INACTIVE",
            ":active": ACTIVE,
            ":now": command.occurredAt,
            ":tokenHash": tokenHash
          }
        }
      });
    }

    if (current !== undefined
      && current.tokenHash !== tokenHash
      && (currentTokenOwner === undefined
        || currentTokenOwner.installationId === command.installationId)) {
      transactionItems.push({
        Delete: {
          TableName: config.tokenOwnersTable,
          Key: { tokenHash: current.tokenHash },
          ConditionExpression: "attribute_not_exists(installationId) OR installationId = :installationId",
          ExpressionAttributeValues: { ":installationId": command.installationId }
        }
      });
    }

    transactionItems.push({
      Put: {
        TableName: config.tokenOwnersTable,
        Item: {
          tokenHash,
          installationId: command.installationId,
          updatedAt: command.occurredAt
        },
        ConditionExpression: owner === undefined
          ? "attribute_not_exists(tokenHash)"
          : "installationId = :expectedInstallationId",
        ...(owner === undefined
          ? {}
          : { ExpressionAttributeValues: { ":expectedInstallationId": owner.installationId } })
      }
    });
    transactionItems.push({
      Put: {
        TableName: config.installationsTable,
        Item: {
          installationId: command.installationId,
          memberId: command.memberId,
          fcmToken: command.fcmToken,
          tokenHash,
          platform: command.platform,
          appVersion: command.appVersion,
          status: ACTIVE,
          registeredAt: command.occurredAt,
          lastValidatedAt: command.occurredAt,
          memberStatusKey: memberStatusKey(command.memberId),
          validationStatusKey: VALIDATION_KEY
        },
        ConditionExpression: current === undefined
          ? "attribute_not_exists(installationId)"
          : "tokenHash = :expectedCurrentTokenHash",
        ...(current === undefined
          ? {}
          : {
              ExpressionAttributeValues: {
                ":expectedCurrentTokenHash": current.tokenHash
              }
            })
      }
    });

    await dynamodb.send(new TransactWriteCommand({ TransactItems: transactionItems }));
  }

  async deactivateOwned(memberId: number, installationId: string, at: string): Promise<boolean> {
    const installation = await this.get(installationId);
    if (installation === undefined || installation.memberId !== memberId || installation.status !== ACTIVE) {
      return false;
    }
    await this.deactivate(installation, at);
    return true;
  }

  async deactivateInvalid(installations: Installation[], at: string): Promise<void> {
    await mapWithConcurrency(installations, 20, async (installation) => {
      if (installation.status === ACTIVE) {
        await this.deactivate(installation, at);
      }
    });
  }

  async listActiveInstallationIds(memberIds: number[]): Promise<string[]> {
    const uniqueMemberIds = [...new Set(memberIds)];
    const results = await mapWithConcurrency(uniqueMemberIds, 20, async (memberId) => {
      const installationIds: string[] = [];
      let cursor: Record<string, unknown> | undefined;
      do {
        const response = await dynamodb.send(new QueryCommand({
          TableName: config.installationsTable,
          IndexName: "member-status-index",
          KeyConditionExpression: "memberStatusKey = :memberStatusKey",
          ExpressionAttributeValues: {
            ":memberStatusKey": memberStatusKey(memberId)
          },
          ProjectionExpression: "installationId",
          ...(cursor === undefined ? {} : { ExclusiveStartKey: cursor })
        }));
        installationIds.push(
          ...(response.Items ?? []).map((item) => String(item.installationId))
        );
        cursor = response.LastEvaluatedKey;
      } while (cursor !== undefined);
      return installationIds;
    });
    return [...new Set(results.flat())];
  }

  async batchGetActive(installationIds: string[]): Promise<Installation[]> {
    const result: Installation[] = [];
    for (const ids of partition([...new Set(installationIds)], 100)) {
      let keys = ids.map((installationId) => ({ installationId }));
      for (let attempt = 0; keys.length > 0 && attempt < 5; attempt++) {
        const response = await dynamodb.send(new BatchGetCommand({
          RequestItems: {
            [config.installationsTable]: {
              Keys: keys,
              ConsistentRead: true
            }
          }
        }));
        const items = response.Responses?.[config.installationsTable] ?? [];
        result.push(...items
          .map((item) => item as Installation)
          .filter((item) => item.status === ACTIVE));
        keys = (response.UnprocessedKeys?.[config.installationsTable]?.Keys ?? [])
          .map((key) => ({ installationId: String(key.installationId) }));
        if (keys.length > 0) {
          await delay(25 * (2 ** attempt));
        }
      }
      if (keys.length > 0) {
        throw new Error("DynamoDB BatchGetItem의 unprocessed key를 모두 처리하지 못했습니다.");
      }
    }
    return result;
  }

  async listValidationTargets(
    validatedBefore: string,
    limit: number,
    exclusiveStartKey?: Record<string, unknown>
  ): Promise<{
    installations: Installation[];
    lastEvaluatedKey?: Record<string, unknown>;
  }> {
    const response = await dynamodb.send(new QueryCommand({
      TableName: config.installationsTable,
      IndexName: "validation-status-index",
      KeyConditionExpression: "validationStatusKey = :active AND lastValidatedAt <= :validatedBefore",
      ExpressionAttributeValues: {
        ":active": VALIDATION_KEY,
        ":validatedBefore": validatedBefore
      },
      Limit: limit,
      ScanIndexForward: true,
      ...(exclusiveStartKey === undefined ? {} : { ExclusiveStartKey: exclusiveStartKey })
    }));
    return {
      installations: (response.Items ?? []).map((item) => item as Installation),
      ...(response.LastEvaluatedKey === undefined
        ? {}
        : { lastEvaluatedKey: response.LastEvaluatedKey })
    };
  }

  async markValidated(installationIds: string[], at: string): Promise<void> {
    await mapWithConcurrency(installationIds, 20, async (installationId) => {
      await dynamodb.send(new UpdateCommand({
        TableName: config.installationsTable,
        Key: { installationId },
        UpdateExpression: "SET lastValidatedAt = :at",
        ConditionExpression: "#status = :active",
        ExpressionAttributeNames: { "#status": "status" },
        ExpressionAttributeValues: { ":at": at, ":active": ACTIVE }
      }));
    });
  }

  private async get(installationId: string): Promise<Installation | undefined> {
    const response = await dynamodb.send(new GetCommand({
      TableName: config.installationsTable,
      Key: { installationId },
      ConsistentRead: true
    }));
    return response.Item as Installation | undefined;
  }

  private async getOwner(tokenHash: string): Promise<{ installationId: string } | undefined> {
    const response = await dynamodb.send(new GetCommand({
      TableName: config.tokenOwnersTable,
      Key: { tokenHash },
      ConsistentRead: true
    }));
    return response.Item as { installationId: string } | undefined;
  }

  private async deactivate(installation: Installation, at: string): Promise<void> {
    await dynamodb.send(new TransactWriteCommand({
      TransactItems: [
        {
          Update: {
            TableName: config.installationsTable,
            Key: { installationId: installation.installationId },
            UpdateExpression: "SET #status = :inactive, deactivatedAt = :at REMOVE memberStatusKey, validationStatusKey",
            ConditionExpression: "#status = :active AND tokenHash = :tokenHash",
            ExpressionAttributeNames: { "#status": "status" },
            ExpressionAttributeValues: {
              ":active": ACTIVE,
              ":inactive": "INACTIVE",
              ":at": at,
              ":tokenHash": installation.tokenHash
            }
          }
        },
        {
          Delete: {
            TableName: config.tokenOwnersTable,
            Key: { tokenHash: installation.tokenHash },
            ConditionExpression: "attribute_not_exists(installationId) OR installationId = :installationId",
            ExpressionAttributeValues: { ":installationId": installation.installationId }
          }
        }
      ]
    }));
  }
}

export const installationRepository = new InstallationRepository();

export function hashToken(token: string): string {
  return createHash("sha256").update(token, "utf8").digest("hex");
}

function memberStatusKey(memberId: number): string {
  return `MEMBER#${memberId}#ACTIVE`;
}

async function mapWithConcurrency<T, R>(
  values: T[],
  concurrency: number,
  mapper: (value: T) => Promise<R>
): Promise<R[]> {
  const result = new Array<R>(values.length);
  let cursor = 0;
  const workers = Array.from({ length: Math.min(concurrency, values.length) }, async () => {
    while (cursor < values.length) {
      const index = cursor++;
      const value = values[index];
      if (value !== undefined) {
        result[index] = await mapper(value);
      }
    }
  });
  await Promise.all(workers);
  return result;
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
