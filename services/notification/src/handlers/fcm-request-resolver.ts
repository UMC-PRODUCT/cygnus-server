import type { SQSEvent, SQSBatchResponse, SQSRecord } from "aws-lambda";
import { SendMessageBatchCommand } from "@aws-sdk/client-sqs";
import { assertValid, validateFcmRequest } from "../contracts/schemas.js";
import type { EventEnvelope, FcmBatchDetail, FcmRequestDetail } from "../contracts/types.js";
import { installationRepository } from "../repositories/installation-repository.js";
import { deliveryRepository } from "../repositories/delivery-repository.js";
import { sqs } from "../shared/aws.js";
import { config } from "../shared/config.js";
import { deterministicUuid } from "../shared/id.js";
import { logger, safeErrorName } from "../shared/observability.js";
import { parseSqsBody, partition } from "../shared/sqs.js";

export async function handler(event: SQSEvent): Promise<SQSBatchResponse> {
  const batchItemFailures: Array<{ itemIdentifier: string }> = [];
  for (const record of event.Records) {
    try {
      await processRecord(record);
    } catch (error) {
      logger.error("FCM request 대상 해석에 실패했습니다.", {
        messageId: record.messageId,
        errorType: safeErrorName(error)
      });
      batchItemFailures.push({ itemIdentifier: record.messageId });
    }
  }
  return { batchItemFailures };
}

async function processRecord(record: SQSRecord): Promise<void> {
  const parsed = parseSqsBody(record);
  assertValid(validateFcmRequest, parsed);
  const request = parsed;
  const claim = await deliveryRepository.claim({
    eventId: request.eventId,
    requestId: request.requestId,
    channel: "FCM"
  }, 1);
  if (claim === "COMPLETED") {
    return;
  }
  if (claim === "IN_PROGRESS") {
    throw new Error("동일한 FCM request event가 이미 처리 중입니다.");
  }

  try {
    const installationIds = await installationRepository.listActiveInstallationIds(
      request.detail.memberIds
    );
    const batches = partition(installationIds, 500);
    await enqueueBatches(request, batches);
    await deliveryRepository.complete(request.eventId, "ACCEPTED");
    logger.info("FCM request를 installation batch로 변환했습니다.", {
      eventId: request.eventId,
      requestId: request.requestId,
      memberCount: request.detail.memberIds.length,
      installationCount: installationIds.length,
      batchCount: batches.length
    });
  } catch (error) {
    await deliveryRepository.release(request.eventId, errorCode(error));
    throw error;
  }
}

async function enqueueBatches(
  request: EventEnvelope<FcmRequestDetail>,
  batches: string[][]
): Promise<void> {
  const queueEntries = batches.map((installationIds, index) => {
    const eventId = deterministicUuid(`${request.eventId}:batch:${index}`);
    const detail: FcmBatchDetail = {
      batchId: `${request.eventId}:${index}`,
      installationIds,
      title: request.detail.title,
      body: request.detail.body,
      data: request.detail.data,
      ...(request.detail.imageUrl !== undefined ? { imageUrl: request.detail.imageUrl } : {}),
      ...(request.detail.deepLink !== undefined ? { deepLink: request.detail.deepLink } : {}),
      attempt: 1
    };
    const batchEvent: EventEnvelope<FcmBatchDetail> = {
      schemaVersion: 1,
      eventId,
      eventType: "notification.fcm.batch.requested.v1",
      source: "umc-product.notification",
      occurredAt: new Date().toISOString(),
      ...(request.traceparent !== undefined ? { traceparent: request.traceparent } : {}),
      requestId: request.requestId,
      detail
    };
    return {
      Id: String(index),
      MessageBody: JSON.stringify(batchEvent)
    };
  });

  for (const entries of partition(queueEntries, 10)) {
    const response = await sqs.send(new SendMessageBatchCommand({
      QueueUrl: config.fcmBatchQueueUrl,
      Entries: entries
    }));
    if ((response.Failed?.length ?? 0) > 0) {
      throw new Error(`FCM batch SQS 발행 실패: ${response.Failed?.[0]?.Code ?? "UNKNOWN"}`);
    }
  }
}

function errorCode(error: unknown): string {
  return error instanceof Error ? error.name : "UNKNOWN";
}
