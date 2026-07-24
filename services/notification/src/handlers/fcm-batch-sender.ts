import type { SQSEvent, SQSBatchResponse, SQSRecord } from "aws-lambda";
import { SendMessageCommand } from "@aws-sdk/client-sqs";
import { assertValid, validateFcmBatch } from "../contracts/schemas.js";
import type { EventEnvelope, FcmBatchDetail } from "../contracts/types.js";
import { classifyFcmError, firebaseMessaging } from "../adapters/firebase.js";
import { deliveryRepository } from "../repositories/delivery-repository.js";
import { installationRepository } from "../repositories/installation-repository.js";
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
      logger.error("FCM batch 발송에 실패했습니다.", {
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
  assertValid(validateFcmBatch, parsed);
  const request = parsed;
  const claim = await deliveryRepository.claim({
    eventId: request.eventId,
    requestId: request.requestId,
    channel: "FCM",
    correlationId: request.detail.batchId
  }, request.detail.attempt);
  if (claim === "COMPLETED") {
    return;
  }
  if (claim === "IN_PROGRESS") {
    throw new Error("동일한 FCM batch event가 이미 처리 중입니다.");
  }

  try {
    const installations = await installationRepository.batchGetActive(request.detail.installationIds);
    if (installations.length === 0) {
      await deliveryRepository.complete(request.eventId, "SENT");
      return;
    }

    const messaging = await firebaseMessaging();
    const response = await messaging.sendEach(
      installations.map((installation) => ({
        token: installation.fcmToken,
        notification: {
          title: request.detail.title,
          body: request.detail.body,
          ...(request.detail.imageUrl ? { imageUrl: request.detail.imageUrl } : {})
        },
        data: {
          ...request.detail.data,
          notificationId: request.requestId,
          ...(request.detail.deepLink ? { deepLink: request.detail.deepLink } : {})
        }
      })),
      config.deliveryMode === "VALIDATE"
    );

    const invalidIndexes: number[] = [];
    const retryableInstallationIds: string[] = [];
    let permanentFailureCount = 0;
    response.responses.forEach((result, index) => {
      if (result.success) {
        return;
      }
      const installation = installations[index];
      if (installation === undefined) {
        return;
      }
      const classification = classifyFcmError(result.error?.code);
      if (classification === "INVALID") {
        invalidIndexes.push(index);
      } else if (classification === "RETRYABLE") {
        retryableInstallationIds.push(installation.installationId);
      } else {
        permanentFailureCount++;
      }
    });

    const invalidInstallations = invalidIndexes
      .map((index) => installations[index])
      .filter((installation) => installation !== undefined);
    await installationRepository.deactivateInvalid(invalidInstallations, new Date().toISOString());
    recordDelivery("FCM", "success", response.successCount);
    recordDelivery("FCM", "failure", response.failureCount);
    recordDelivery("FCM", "invalid", invalidInstallations.length);

    if (retryableInstallationIds.length > 0) {
      if (request.detail.attempt < 5) {
        await enqueueRetry(request, retryableInstallationIds);
        recordDelivery("FCM", "retry", retryableInstallationIds.length);
      } else {
        recordDelivery("FCM", "failure", retryableInstallationIds.length);
      }
    }

    const exhausted = retryableInstallationIds.length > 0 && request.detail.attempt >= 5;
    await deliveryRepository.complete(
      request.eventId,
      exhausted || permanentFailureCount > 0
        ? "FAILED"
        : config.deliveryMode === "LIVE" ? "SENT" : "VALIDATED"
    );
    logger.info("FCM batch 처리를 완료했습니다.", {
      eventId: request.eventId,
      requestId: request.requestId,
      targetCount: installations.length,
      successCount: response.successCount,
      failureCount: response.failureCount,
      retryCount: retryableInstallationIds.length,
      permanentFailureCount
    });
  } catch (error) {
    await deliveryRepository.release(request.eventId, error instanceof Error ? error.name : "UNKNOWN");
    throw error;
  }
}

async function enqueueRetry(
  request: EventEnvelope<FcmBatchDetail>,
  installationIds: string[]
): Promise<void> {
  const nextAttempt = request.detail.attempt + 1;
  const retryEvent: EventEnvelope<FcmBatchDetail> = {
    ...request,
    eventId: deterministicUuid(`${request.eventId}:retry:${nextAttempt}`),
    occurredAt: new Date().toISOString(),
    detail: {
      ...request.detail,
      installationIds,
      attempt: nextAttempt
    }
  };
  await sqs.send(new SendMessageCommand({
    QueueUrl: config.fcmBatchQueueUrl,
    MessageBody: JSON.stringify(retryEvent),
    DelaySeconds: RETRY_DELAYS[Math.min(request.detail.attempt - 1, RETRY_DELAYS.length - 1)]
  }));
}
