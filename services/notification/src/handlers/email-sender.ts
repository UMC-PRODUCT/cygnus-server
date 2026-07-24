import type { SQSEvent, SQSBatchResponse, SQSRecord } from "aws-lambda";
import { SendEmailCommand } from "@aws-sdk/client-sesv2";
import {
  assertValid,
  validateRecruitingEmail,
  validateVerificationEmail
} from "../contracts/schemas.js";
import {
  EVENT_TYPES,
  type EventEnvelope,
  type RecruitingEmailDetail,
  type VerificationEmailDetail
} from "../contracts/types.js";
import { isVerificationExpired } from "../domain/email.js";
import { publishEmailResult } from "../adapters/delivery-result-publisher.js";
import {
  renderRecruitingEmail,
  renderVerificationEmail
} from "../adapters/email-templates.js";
import { deliveryRepository } from "../repositories/delivery-repository.js";
import { ses } from "../shared/aws.js";
import { config } from "../shared/config.js";
import { logger, metrics, recordDelivery, safeErrorName } from "../shared/observability.js";
import { parseSqsBody, receiveCount } from "../shared/sqs.js";

type EmailRequest =
  | EventEnvelope<VerificationEmailDetail>
  | EventEnvelope<RecruitingEmailDetail>;

export async function handler(event: SQSEvent): Promise<SQSBatchResponse> {
  const batchItemFailures: Array<{ itemIdentifier: string }> = [];
  for (const record of event.Records) {
    try {
      await processRecord(record);
    } catch (error) {
      logger.error("Email 발송에 실패했습니다.", {
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
  const request = parseRequest(parseSqsBody(record));
  const correlation = correlationOf(request);
  const pendingResult = await deliveryRepository.getPendingResult(request.eventId);
  if (pendingResult !== undefined) {
    await publishAcceptedResult(request, correlation, pendingResult.providerMessageId);
    await deliveryRepository.complete(request.eventId, "ACCEPTED", pendingResult.providerMessageId);
    recordDelivery("EMAIL", "success");
    return;
  }

  const claim = await deliveryRepository.claim({
    eventId: request.eventId,
    requestId: request.requestId,
    channel: "EMAIL",
    correlationId: correlation.id
  }, receiveCount(record));
  if (claim === "COMPLETED") {
    return;
  }
  if (claim === "IN_PROGRESS") {
    throw new Error("동일한 email event가 이미 처리 중입니다.");
  }

  if (isExpiredVerification(request)) {
    await deliveryRepository.complete(request.eventId, "EXPIRED");
    await publishEmailResult(request, {
      correlationType: correlation.type,
      correlationId: correlation.id,
      status: "EXPIRED",
      failureCode: "VERIFICATION_EXPIRED",
      attemptedAt: new Date().toISOString()
    });
    recordDelivery("EMAIL", "expired");
    return;
  }

  const content = emailContent(request);
  if (config.deliveryMode === "VALIDATE") {
    await deliveryRepository.complete(request.eventId, "VALIDATED");
    return;
  }

  let resultPending = false;
  try {
    const response = await ses.send(new SendEmailCommand({
      FromEmailAddress: `${config.emailFromName} <${config.emailFromAddress}>`,
      Destination: { ToAddresses: [content.to] },
      Content: {
        Simple: {
          Subject: { Data: content.subject, Charset: "UTF-8" },
          Body: { Html: { Data: content.html, Charset: "UTF-8" } }
        }
      },
      ...(config.sesConfigurationSet
        ? { ConfigurationSetName: config.sesConfigurationSet }
        : {})
    }));
    await deliveryRepository.markResultPending(request.eventId, response.MessageId);
    resultPending = true;
    await publishAcceptedResult(request, correlation, response.MessageId);
    await deliveryRepository.complete(request.eventId, "ACCEPTED", response.MessageId);
    recordDelivery("EMAIL", "success");
    logger.info("Email provider가 요청을 수락했습니다.", {
      eventId: request.eventId,
      requestId: request.requestId,
      correlationType: correlation.type
    });
  } catch (error) {
    if (resultPending) {
      throw error;
    }
    const terminal = isTerminalEmailError(error) || receiveCount(record) >= 5;
    if (terminal) {
      const failureCode = awsErrorCode(error);
      await deliveryRepository.complete(request.eventId, "FAILED");
      await publishEmailResult(request, {
        correlationType: correlation.type,
        correlationId: correlation.id,
        status: "FAILED",
        failureCode,
        attemptedAt: new Date().toISOString()
      });
      recordDelivery("EMAIL", "failure");
      return;
    }
    await deliveryRepository.release(request.eventId, awsErrorCode(error));
    throw error;
  }
}

async function publishAcceptedResult(
  request: EmailRequest,
  correlation: ReturnType<typeof correlationOf>,
  providerMessageId?: string
): Promise<void> {
  await publishEmailResult(request, {
    correlationType: correlation.type,
    correlationId: correlation.id,
    status: "ACCEPTED",
    providerMessageId: providerMessageId ?? null,
    attemptedAt: new Date().toISOString()
  });
}

function parseRequest(value: unknown): EmailRequest {
  if (typeof value !== "object" || value === null || !("eventType" in value)) {
    throw new Error("Email event envelope이 아닙니다.");
  }
  if (value.eventType === EVENT_TYPES.verificationEmailRequested) {
    assertValid(validateVerificationEmail, value);
    return value;
  }
  if (value.eventType === EVENT_TYPES.recruitingEmailRequested) {
    assertValid(validateRecruitingEmail, value);
    return value;
  }
  throw new Error("지원하지 않는 email eventType입니다.");
}

function emailContent(request: EmailRequest): { to: string; subject: string; html: string } {
  if (request.eventType === EVENT_TYPES.verificationEmailRequested) {
    const detail = request.detail as VerificationEmailDetail;
    return {
      to: detail.email,
      subject: "UMC 이메일 인증 코드 안내",
      html: renderVerificationEmail(detail.verificationCode)
    };
  }
  const detail = request.detail as RecruitingEmailDetail;
  return {
    to: detail.email,
    subject: "UMC 면접 가능 일정 제출 안내",
    html: renderRecruitingEmail(detail)
  };
}

function correlationOf(request: EmailRequest): {
  type: "EMAIL_VERIFICATION" | "RECRUITING_INTERVIEW";
  id: string;
} {
  if (request.eventType === EVENT_TYPES.verificationEmailRequested) {
    return { type: "EMAIL_VERIFICATION", id: request.requestId };
  }
  return {
    type: "RECRUITING_INTERVIEW",
    id: String((request.detail as RecruitingEmailDetail).applicationId)
  };
}

function isExpiredVerification(request: EmailRequest): boolean {
  return request.eventType === EVENT_TYPES.verificationEmailRequested
    && isVerificationExpired((request.detail as VerificationEmailDetail).expiresAt);
}

function isTerminalEmailError(error: unknown): boolean {
  const code = awsErrorCode(error);
  return code === "MessageRejected"
    || code === "MailFromDomainNotVerifiedException"
    || code === "AccountSuspendedException";
}

function awsErrorCode(error: unknown): string {
  if (typeof error === "object" && error !== null && "name" in error && typeof error.name === "string") {
    return error.name.slice(0, 100);
  }
  return "UNKNOWN";
}
