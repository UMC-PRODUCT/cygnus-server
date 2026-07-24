import {
  GetCommand,
  PutCommand,
  UpdateCommand
} from "@aws-sdk/lib-dynamodb";
import { ConditionalCheckFailedException } from "@aws-sdk/client-dynamodb";
import { config } from "../shared/config.js";
import { dynamodb } from "../shared/aws.js";

const THIRTY_DAYS_IN_SECONDS = 30 * 24 * 60 * 60;
const LEASE_SECONDS = 10 * 60;

export type ClaimResult = "ACQUIRED" | "COMPLETED" | "IN_PROGRESS";

export interface DeliveryIdentity {
  eventId: string;
  requestId: string;
  channel: "FCM" | "EMAIL" | "WEBHOOK";
  correlationId?: string;
}

export interface PendingDeliveryResult {
  providerMessageId?: string;
}

export class DeliveryRepository {
  async claim(identity: DeliveryIdentity, attempt: number): Promise<ClaimResult> {
    const now = Math.floor(Date.now() / 1000);
    try {
      await dynamodb.send(new PutCommand({
        TableName: config.deliveriesTable,
        Item: {
          ...identity,
          status: "PROCESSING",
          attempt,
          leaseUntil: now + LEASE_SECONDS,
          createdAt: new Date().toISOString(),
          expiresAt: now + THIRTY_DAYS_IN_SECONDS
        },
        ConditionExpression: "attribute_not_exists(eventId) OR leaseUntil < :now OR #status = :retryable",
        ExpressionAttributeNames: { "#status": "status" },
        ExpressionAttributeValues: {
          ":now": now,
          ":retryable": "RETRYABLE"
        }
      }));
      return "ACQUIRED";
    } catch (error) {
      if (!(error instanceof ConditionalCheckFailedException)) {
        throw error;
      }
      const current = await this.get(identity.eventId);
      if (current?.status === "PROCESSING") {
        return "IN_PROGRESS";
      }
      return "COMPLETED";
    }
  }

  async complete(
    eventId: string,
    status: "ACCEPTED" | "SENT" | "VALIDATED" | "FAILED" | "EXPIRED",
    providerMessageId?: string
  ): Promise<void> {
    await dynamodb.send(new UpdateCommand({
      TableName: config.deliveriesTable,
      Key: { eventId },
      UpdateExpression: "SET #status = :status, completedAt = :completedAt, providerMessageId = :providerMessageId REMOVE leaseUntil",
      ExpressionAttributeNames: { "#status": "status" },
      ExpressionAttributeValues: {
        ":status": status,
        ":completedAt": new Date().toISOString(),
        ":providerMessageId": providerMessageId ?? null
      }
    }));
  }

  async markResultPending(eventId: string, providerMessageId?: string): Promise<void> {
    await dynamodb.send(new UpdateCommand({
      TableName: config.deliveriesTable,
      Key: { eventId },
      UpdateExpression: "SET #status = :status, providerMessageId = :providerMessageId REMOVE leaseUntil",
      ExpressionAttributeNames: { "#status": "status" },
      ExpressionAttributeValues: {
        ":status": "RESULT_PENDING",
        ":providerMessageId": providerMessageId ?? null
      }
    }));
  }

  async getPendingResult(eventId: string): Promise<PendingDeliveryResult | undefined> {
    const current = await this.get(eventId);
    if (current?.status !== "RESULT_PENDING") {
      return undefined;
    }
    return {
      ...(current.providerMessageId === undefined || current.providerMessageId === null
        ? {}
        : { providerMessageId: current.providerMessageId })
    };
  }

  async release(eventId: string, failureCode: string): Promise<void> {
    await dynamodb.send(new UpdateCommand({
      TableName: config.deliveriesTable,
      Key: { eventId },
      UpdateExpression: "SET #status = :retryable, failureCode = :failureCode REMOVE leaseUntil",
      ExpressionAttributeNames: { "#status": "status" },
      ExpressionAttributeValues: {
        ":retryable": "RETRYABLE",
        ":failureCode": failureCode.slice(0, 100)
      }
    }));
  }

  private async get(eventId: string): Promise<{
    status: string;
    providerMessageId?: string | null;
  } | undefined> {
    const response = await dynamodb.send(new GetCommand({
      TableName: config.deliveriesTable,
      Key: { eventId },
      ConsistentRead: true
    }));
    return response.Item as {
      status: string;
      providerMessageId?: string | null;
    } | undefined;
  }
}

export const deliveryRepository = new DeliveryRepository();
