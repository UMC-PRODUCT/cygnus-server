import type { ScheduledHandler } from "aws-lambda";
import { firebaseMessaging, classifyFcmError } from "../adapters/firebase.js";
import type { Installation } from "../domain/installation.js";
import { installationRepository } from "../repositories/installation-repository.js";
import { logger, metrics, recordDelivery } from "../shared/observability.js";

const STALE_DAYS = 30;
const BATCH_SIZE = 500;

export const handler: ScheduledHandler = async (_event, context) => {
  const now = new Date();
  const validatedBefore = new Date(now.getTime() - STALE_DAYS * 24 * 60 * 60 * 1000).toISOString();
  const messaging = await firebaseMessaging();
  let cursor: Record<string, unknown> | undefined;
  let requestedCount = 0;
  let validCount = 0;
  let invalidCount = 0;
  do {
    const page = await installationRepository.listValidationTargets(
      validatedBefore,
      BATCH_SIZE,
      cursor
    );
    if (page.installations.length === 0) {
      break;
    }
    const result = await validatePage(page.installations, now.toISOString(), messaging);
    requestedCount += page.installations.length;
    validCount += result.validCount;
    invalidCount += result.invalidCount;
    cursor = page.lastEvaluatedKey;
  } while (cursor !== undefined && context.getRemainingTimeInMillis() > 10_000);

  recordDelivery("FCM", "success", validCount);
  recordDelivery("FCM", "invalid", invalidCount);
  logger.info("FCM token 유효성 검증을 완료했습니다.", {
    requestedCount,
    validCount,
    invalidCount,
    continuationRemaining: cursor !== undefined
  });
  metrics.publishStoredMetrics();
};

async function validatePage(
  installations: Installation[],
  validatedAt: string,
  messaging: Awaited<ReturnType<typeof firebaseMessaging>>
): Promise<{ validCount: number; invalidCount: number }> {
  const response = await messaging.sendEach(
    installations.map((installation) => ({
      token: installation.fcmToken,
      data: { validation: "true" }
    })),
    true
  );
  const validIds: string[] = [];
  const invalidInstallations: Installation[] = [];
  response.responses.forEach((result, index) => {
    const installation = installations[index];
    if (installation === undefined) {
      return;
    }
    if (result.success) {
      validIds.push(installation.installationId);
    } else if (classifyFcmError(result.error?.code) === "INVALID") {
      invalidInstallations.push(installation);
    }
  });
  await Promise.all([
    installationRepository.markValidated(validIds, validatedAt),
    installationRepository.deactivateInvalid(invalidInstallations, validatedAt)
  ]);
  return {
    validCount: validIds.length,
    invalidCount: invalidInstallations.length
  };
}
