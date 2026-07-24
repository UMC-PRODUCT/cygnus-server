import { beforeEach, describe, expect, it, vi } from "vitest";
import { dynamodb } from "../src/shared/aws.js";
import {
  hashToken,
  InstallationRepository
} from "../src/repositories/installation-repository.js";

describe("InstallationRepository", () => {
  beforeEach(() => {
    process.env.INSTALLATIONS_TABLE = "installations";
    process.env.TOKEN_OWNERS_TABLE = "token-owners";
    vi.restoreAllMocks();
  });

  it("raw token 대신 SHA-256 hash를 owner key로 사용한다", () => {
    expect(hashToken("secret-token")).toBe(
      "930bbdc51b6aed5c2a5678fd6e28dee7a05e8a4b643cfc0b4427c3efb86c0d94"
    );
  });

  it("새 installation과 token owner를 하나의 transaction으로 등록한다", async () => {
    const send = vi.spyOn(dynamodb, "send") as unknown as {
      mockResolvedValueOnce(value: unknown): unknown;
      mockResolvedValue(value: unknown): unknown;
      mock: { calls: Array<[unknown]> };
    };
    send.mockResolvedValueOnce({});
    send.mockResolvedValueOnce({});
    send.mockResolvedValue({});
    const repository = new InstallationRepository();

    await repository.register({
      memberId: 1,
      installationId: "installation-1",
      fcmToken: "token-1",
      platform: "IOS",
      appVersion: "1.0.0",
      occurredAt: "2026-07-23T10:00:00.000Z"
    });

    const transaction = send.mock.calls[2]?.[0] as {
      input: { TransactItems: Array<Record<string, unknown>> };
    };
    expect(transaction.input.TransactItems).toHaveLength(2);
    expect(transaction.input.TransactItems[0]).toHaveProperty("Put");
    expect(transaction.input.TransactItems[1]).toHaveProperty("Put");
    expect(JSON.stringify(transaction.input)).not.toContain("\"tokenHash\":\"token-1\"");
  });

  it("이전 token 소유권이 이미 이동했으면 다른 installation의 owner를 삭제하지 않는다", async () => {
    const send = vi.spyOn(dynamodb, "send") as unknown as {
      mockResolvedValueOnce(value: unknown): unknown;
      mockResolvedValue(value: unknown): unknown;
      mock: { calls: Array<[unknown]> };
    };
    const oldTokenHash = hashToken("old-token");
    send.mockResolvedValueOnce({});
    send.mockResolvedValueOnce({
      Item: {
        installationId: "installation-1",
        memberId: 1,
        fcmToken: "old-token",
        tokenHash: oldTokenHash,
        status: "INACTIVE"
      }
    });
    send.mockResolvedValueOnce({
      Item: { tokenHash: oldTokenHash, installationId: "installation-2" }
    });
    send.mockResolvedValue({});
    const repository = new InstallationRepository();

    await repository.register({
      memberId: 1,
      installationId: "installation-1",
      fcmToken: "new-token",
      occurredAt: "2026-07-23T10:00:00.000Z"
    });

    const transaction = send.mock.calls[3]?.[0] as {
      input: { TransactItems: Array<Record<string, unknown>> };
    };
    expect(transaction.input.TransactItems).toHaveLength(2);
    expect(transaction.input.TransactItems).not.toContainEqual(
      expect.objectContaining({ Delete: expect.anything() })
    );
  });
});
