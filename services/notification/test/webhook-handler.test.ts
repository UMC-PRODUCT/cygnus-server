import type { SQSRecord } from "aws-lambda";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  claim: vi.fn(),
  complete: vi.fn(),
  release: vi.fn(),
  sendWebhook: vi.fn(),
  sqsSend: vi.fn(),
  recordDelivery: vi.fn()
}));

vi.mock("../src/repositories/delivery-repository.js", () => ({
  deliveryRepository: {
    claim: mocks.claim,
    complete: mocks.complete,
    release: mocks.release
  }
}));
vi.mock("../src/adapters/webhook-client.js", () => {
  class WebhookHttpError extends Error {
    constructor(readonly status: number) {
      super(`HTTP ${status}`);
    }

    get retryable(): boolean {
      return this.status === 408 || this.status === 429 || this.status >= 500;
    }
  }
  return { sendWebhook: mocks.sendWebhook, WebhookHttpError };
});
vi.mock("../src/shared/aws.js", () => ({
  sqs: { send: mocks.sqsSend }
}));
vi.mock("../src/shared/observability.js", () => ({
  logger: { info: vi.fn(), error: vi.fn() },
  metrics: { publishStoredMetrics: vi.fn() },
  recordDelivery: mocks.recordDelivery,
  safeErrorName: (error: unknown) => error instanceof Error ? error.name : typeof error
}));

describe("Webhook sender", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    process.env.DELIVERY_MODE = "LIVE";
    mocks.claim.mockResolvedValue("ACQUIRED");
    mocks.complete.mockResolvedValue(undefined);
  });

  it("한 provider의 영구 실패가 다른 provider 전송을 막지 않는다", async () => {
    const { WebhookHttpError } = await import("../src/adapters/webhook-client.js");
    mocks.sendWebhook.mockImplementation(async (platform: string) => {
      if (platform === "SLACK") {
        throw new WebhookHttpError(400);
      }
    });
    const { handler } = await import("../src/handlers/webhook-sender.js");

    const response = await handler({ Records: [record()] });

    expect(response.batchItemFailures).toEqual([]);
    expect(mocks.sendWebhook).toHaveBeenCalledTimes(2);
    expect(mocks.sendWebhook).toHaveBeenCalledWith("SLACK", "제목", "내용");
    expect(mocks.sendWebhook).toHaveBeenCalledWith("DISCORD", "제목", "내용");
    expect(mocks.complete).toHaveBeenCalledWith(
      "11111111-1111-4111-8111-111111111111",
      "SENT"
    );
  });

  it("이미 완료한 event는 provider를 다시 호출하지 않는다", async () => {
    mocks.claim.mockResolvedValue("COMPLETED");
    const { handler } = await import("../src/handlers/webhook-sender.js");

    await handler({ Records: [record()] });

    expect(mocks.sendWebhook).not.toHaveBeenCalled();
  });
});

function record(): SQSRecord {
  return {
    messageId: "message-1",
    receiptHandle: "receipt-1",
    body: JSON.stringify({
      schemaVersion: 1,
      eventId: "11111111-1111-4111-8111-111111111111",
      eventType: "notification.webhook.requested.v1",
      source: "umc-product.notification-gateway",
      occurredAt: "2026-07-23T00:00:00.000Z",
      requestId: "22222222-2222-4222-8222-222222222222",
      detail: {
        platforms: ["SLACK", "DISCORD"],
        title: "제목",
        content: "내용",
        attempt: 1
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
    eventSourceARN: "arn:aws:sqs:ap-northeast-2:123456789012:test",
    awsRegion: "ap-northeast-2"
  };
}
