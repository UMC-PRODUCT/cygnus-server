import type { SQSEvent, SQSBatchResponse, SQSRecord } from "aws-lambda";
import { SendMessageCommand } from "@aws-sdk/client-sqs";
import { assertValid, validateWebhook } from "../contracts/schemas.js";
import type { EventEnvelope, WebhookDetail, WebhookPlatform } from "../contracts/types.js";
import { sendWebhook, WebhookHttpError } from "../adapters/webhook-client.js";
import { deliveryRepository } from "../repositories/delivery-repository.js";
import { sqs } from "../shared/aws.js";
import { config } from "../shared/config.js";
import { deterministicUuid } from "../shared/id.js";
import { logger, metrics, recordDelivery, safeErrorName } from "../shared/observability.js";
import { parseSqsBody } from "../shared/sqs.js";

const RETRY_DELAYS = [10, 60, 300, 900] as const;

export async function handler(event: SQSEvent): Promise<SQSBatchResponse> {
  const batchItemFailures: Array<{ itemIdentifier: string }> = [];
  for (const record of event.Records) {
    try {
      await processRecord(record);
    } catch (error) {
      logger.error("Webhook 전송에 실패했습니다.", {
        messageId: record.messageId,
        errorType: safeErrorName(error)
      });
      batchItemFailures.push({ itemIdentifier: record.messageId });
    }
  }
  metrics.publishStoredMetrics();
  return { batchItemFailures };
}

async function processRecord(record: SQSRecord): Promise<void> {
  const parsed = parseSqsBody(record);
  assertValid(validateWebhook, parsed);
  const request = parsed;
  const claim = await deliveryRepository.claim({
    eventId: request.eventId,
    requestId: request.requestId,
    channel: "WEBHOOK"
  }, request.detail.attempt);
  if (claim === "COMPLETED") {
    return;
  }
  if (claim === "IN_PROGRESS") {
    throw new Error("동일한 webhook event가 이미 처리 중입니다.");
  }

  if (config.deliveryMode === "VALIDATE") {
    await deliveryRepository.complete(request.eventId, "VALIDATED");
    return;
  }

  try {
    const retryable: WebhookPlatform[] = [];
    for (const platform of request.detail.platforms) {
      try {
        await sendWebhook(platform, request.detail.title, request.detail.content);
        recordDelivery("WEBHOOK", "success");
      } catch (error) {
        if (isRetryable(error)) {
          retryable.push(platform);
        } else {
          recordDelivery("WEBHOOK", "failure");
        }
      }
    }
    if (retryable.length > 0 && request.detail.attempt < 5) {
      await enqueueRetry(request, retryable);
      recordDelivery("WEBHOOK", "retry", retryable.length);
    } else if (retryable.length > 0) {
      recordDelivery("WEBHOOK", "failure", retryable.length);
    }
    await deliveryRepository.complete(
      request.eventId,
      retryable.length > 0 && request.detail.attempt >= 5 ? "FAILED" : "SENT"
    );
    logger.info("Webhook event 처리를 완료했습니다.", {
      eventId: request.eventId,
      requestId: request.requestId,
      platformCount: request.detail.platforms.length,
      retryCount: retryable.length
    });
  } catch (error) {
    await deliveryRepository.release(request.eventId, error instanceof Error ? error.name : "UNKNOWN");
    throw error;
  }
}

async function enqueueRetry(
  request: EventEnvelope<WebhookDetail>,
  platforms: WebhookPlatform[]
): Promise<void> {
  const nextAttempt = request.detail.attempt + 1;
  const retryEvent: EventEnvelope<WebhookDetail> = {
    ...request,
    eventId: deterministicUuid(
      `${request.eventId}:retry:${nextAttempt}:${[...platforms].sort().join(",")}`
    ),
    occurredAt: new Date().toISOString(),
    detail: {
      ...request.detail,
      platforms,
      attempt: nextAttempt
    }
  };
  await sqs.send(new SendMessageCommand({
    QueueUrl: config.webhookQueueUrl,
    MessageBody: JSON.stringify(retryEvent),
    DelaySeconds: RETRY_DELAYS[Math.min(request.detail.attempt - 1, RETRY_DELAYS.length - 1)]
  }));
}

function isRetryable(error: unknown): boolean {
  return error instanceof WebhookHttpError
    ? error.retryable
    : true;
}
