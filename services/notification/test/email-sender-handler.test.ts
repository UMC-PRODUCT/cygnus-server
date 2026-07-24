import type { SQSRecord } from "aws-lambda";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  claim: vi.fn(),
  complete: vi.fn(),
  getPendingResult: vi.fn(),
  markResultPending: vi.fn(),
  release: vi.fn(),
  publishEmailResult: vi.fn(),
  sesSend: vi.fn(),
  recordDelivery: vi.fn()
}));

vi.mock("../src/repositories/delivery-repository.js", () => ({
  deliveryRepository: {
    claim: mocks.claim,
    complete: mocks.complete,
    getPendingResult: mocks.getPendingResult,
    markResultPending: mocks.markResultPending,
    release: mocks.release
  }
}));
vi.mock("../src/adapters/delivery-result-publisher.js", () => ({
  publishEmailResult: mocks.publishEmailResult
}));
vi.mock("../src/shared/aws.js", () => ({
  ses: { send: mocks.sesSend }
}));
vi.mock("../src/shared/config.js", () => ({
  config: {
    deliveryMode: "LIVE",
    emailFromName: "UMC",
    emailFromAddress: "no-reply@example.com",
    sesConfigurationSet: undefined
  }
}));
vi.mock("../src/shared/observability.js", () => ({
  logger: { info: vi.fn(), error: vi.fn() },
  metrics: { publishStoredMetrics: vi.fn() },
  recordDelivery: mocks.recordDelivery,
  safeErrorName: (error: unknown) => error instanceof Error ? error.name : typeof error
}));

describe("Email sender", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.claim.mockResolvedValue("ACQUIRED");
    mocks.complete.mockResolvedValue(undefined);
    mocks.getPendingResult.mockResolvedValue(undefined);
    mocks.markResultPending.mockResolvedValue(undefined);
    mocks.publishEmailResult.mockResolvedValue(undefined);
    mocks.sesSend.mockResolvedValue({ MessageId: "ses-message-1" });
  });

  it("SES 수락 후 결과 발행이 실패하면 RESULT_PENDING 상태를 보존한다", async () => {
    mocks.publishEmailResult.mockRejectedValueOnce(new Error("EventBridge unavailable"));
    const { handler } = await import("../src/handlers/email-sender.js");

    const response = await handler({ Records: [record()] });

    expect(response.batchItemFailures).toEqual([{ itemIdentifier: "message-1" }]);
    expect(mocks.sesSend).toHaveBeenCalledOnce();
    expect(mocks.markResultPending).toHaveBeenCalledWith(
      "11111111-1111-4111-8111-111111111111",
      "ses-message-1"
    );
    expect(mocks.release).not.toHaveBeenCalled();
  });

  it("RESULT_PENDING 재시도는 SES를 다시 호출하지 않고 결과만 발행한다", async () => {
    mocks.getPendingResult.mockResolvedValue({ providerMessageId: "ses-message-1" });
    const { handler } = await import("../src/handlers/email-sender.js");

    const response = await handler({ Records: [record()] });

    expect(response.batchItemFailures).toEqual([]);
    expect(mocks.sesSend).not.toHaveBeenCalled();
    expect(mocks.publishEmailResult).toHaveBeenCalledOnce();
    expect(mocks.complete).toHaveBeenCalledWith(
      "11111111-1111-4111-8111-111111111111",
      "ACCEPTED",
      "ses-message-1"
    );
  });
});

function record(): SQSRecord {
  return {
    messageId: "message-1",
    receiptHandle: "receipt-message-1",
    body: JSON.stringify({
      schemaVersion: 1,
      eventId: "11111111-1111-4111-8111-111111111111",
      eventType: "recruiting.interview.email.requested.v1",
      source: "umc-product.recruiting",
      occurredAt: "2026-07-23T00:00:00.000Z",
      requestId: "22222222-2222-4222-8222-222222222222",
      detail: {
        applicationId: 10,
        email: "applicant@example.com",
        applicantName: "지원자",
        availabilityFormId: 20,
        contactText: "문의"
      }
    }),
    attributes: {
      ApproximateReceiveCount: "1",
      SentTimestamp: "0",
      SenderId: "test",
      ApproximateFirstReceiveTimestamp: "0"
    },
    messageAttributes: {},
    md5OfBody: "",
    eventSource: "aws:sqs",
    eventSourceARN: "arn:aws:sqs:ap-northeast-2:123456789012:email",
    awsRegion: "ap-northeast-2"
  };
}
