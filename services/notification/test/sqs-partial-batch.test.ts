import type { SQSRecord } from "aws-lambda";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  claim: vi.fn(),
  complete: vi.fn(),
  release: vi.fn(),
  listActiveInstallationIds: vi.fn(),
  sqsSend: vi.fn()
}));

vi.mock("../src/repositories/delivery-repository.js", () => ({
  deliveryRepository: {
    claim: mocks.claim,
    complete: mocks.complete,
    release: mocks.release
  }
}));
vi.mock("../src/repositories/installation-repository.js", () => ({
  installationRepository: {
    listActiveInstallationIds: mocks.listActiveInstallationIds
  }
}));
vi.mock("../src/shared/aws.js", () => ({
  sqs: { send: mocks.sqsSend }
}));
vi.mock("../src/shared/observability.js", () => ({
  logger: { info: vi.fn(), error: vi.fn() },
  safeErrorName: (error: unknown) => error instanceof Error ? error.name : typeof error
}));

describe("SQS partial batch failure", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    process.env.FCM_BATCH_QUEUE_URL = "https://sqs.example.com/fcm-batch";
    mocks.claim.mockResolvedValue("ACQUIRED");
    mocks.listActiveInstallationIds.mockResolvedValue([]);
    mocks.complete.mockResolvedValue(undefined);
  });

  it("정상 record는 완료하고 계약 위반 record만 batchItemFailures로 반환한다", async () => {
    const { handler } = await import("../src/handlers/fcm-request-resolver.js");
    const valid = record("valid", {
      schemaVersion: 1,
      eventId: "11111111-1111-4111-8111-111111111111",
      eventType: "notification.fcm.requested.v1",
      source: "umc-product.notification-gateway",
      occurredAt: "2026-07-23T00:00:00.000Z",
      requestId: "22222222-2222-4222-8222-222222222222",
      detail: {
        chunkIndex: 0,
        chunkCount: 1,
        memberIds: [1],
        title: "공지",
        body: "본문",
        data: {}
      }
    });
    const invalid = record("invalid", { eventType: "unknown" });

    const response = await handler({ Records: [valid, invalid] });

    expect(response.batchItemFailures).toEqual([{ itemIdentifier: "invalid" }]);
    expect(mocks.complete).toHaveBeenCalledWith(
      "11111111-1111-4111-8111-111111111111",
      "ACCEPTED"
    );
  });
});

function record(messageId: string, body: unknown): SQSRecord {
  return {
    messageId,
    receiptHandle: `receipt-${messageId}`,
    body: JSON.stringify(body),
    attributes: {
      ApproximateReceiveCount: "1",
      SentTimestamp: "0",
      SenderId: "test",
      ApproximateFirstReceiveTimestamp: "0"
    },
    messageAttributes: {},
    md5OfBody: "",
    eventSource: "aws:sqs",
    eventSourceARN: "arn:aws:sqs:ap-northeast-2:123456789012:test",
    awsRegion: "ap-northeast-2"
  };
}
