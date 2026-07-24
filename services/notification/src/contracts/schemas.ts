import { Ajv, type JSONSchemaType, type ValidateFunction } from "ajv";
import addFormats from "ajv-formats";
import {
  authenticationEmailVerificationRequestedSchema,
  notificationFcmRequestedSchema,
  notificationEmailResultSchema,
  notificationWebhookRequestedSchema,
  recruitingInterviewEmailRequestedSchema
} from "./generated-schemas.js";
import type {
  EventEnvelope,
  EmailResultDetail,
  FcmBatchDetail,
  FcmInstallationCommand,
  FcmRequestDetail,
  RecruitingEmailDetail,
  VerificationEmailDetail,
  WebhookDetail
} from "./types.js";

const uuid = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

const envelopeProperties = {
  schemaVersion: { type: "integer", const: 1 },
  eventId: { type: "string", pattern: uuid },
  eventType: { type: "string", minLength: 1 },
  source: { type: "string", minLength: 1 },
  occurredAt: { type: "string", format: "date-time" },
  traceparent: { type: ["string", "null"], maxLength: 64, nullable: true },
  requestId: { type: "string", pattern: uuid }
} as const;

const fcmBatchSchema = {
  type: "object",
  required: ["schemaVersion", "eventId", "eventType", "source", "occurredAt", "requestId", "detail"],
  additionalProperties: false,
  properties: {
    ...envelopeProperties,
    eventType: { type: "string", const: "notification.fcm.batch.requested.v1" },
    detail: {
      type: "object",
      required: ["batchId", "installationIds", "title", "body", "data", "attempt"],
      additionalProperties: false,
      properties: {
        batchId: { type: "string", minLength: 1, maxLength: 200 },
        installationIds: {
          type: "array",
          minItems: 1,
          maxItems: 500,
          uniqueItems: true,
          items: { type: "string", minLength: 1, maxLength: 100 }
        },
        title: { type: "string", minLength: 1, maxLength: 200 },
        body: { type: "string", minLength: 1, maxLength: 2000 },
        data: {
          type: "object",
          additionalProperties: { type: "string" },
          required: []
        },
        imageUrl: { type: ["string", "null"], format: "uri", nullable: true },
        deepLink: { type: ["string", "null"], maxLength: 2048, nullable: true },
        attempt: { type: "integer", minimum: 1, maximum: 5 }
      }
    }
  }
} as const;

const installationCommandSchema: JSONSchemaType<FcmInstallationCommand> = {
  type: "object",
  required: ["commandId", "action", "occurredAt", "memberId", "installationId"],
  additionalProperties: false,
  properties: {
    commandId: { type: "string", pattern: uuid },
    action: { type: "string", enum: ["UPSERT", "DEACTIVATE"] },
    occurredAt: { type: "string", format: "date-time" },
    memberId: { type: "integer", minimum: 1 },
    installationId: { type: "string", minLength: 1, maxLength: 100 },
    fcmToken: { type: "string", minLength: 1, maxLength: 4096, nullable: true },
    platform: { type: "string", maxLength: 30, nullable: true },
    appVersion: { type: "string", maxLength: 50, nullable: true }
  }
};

const ajv = new Ajv({ allErrors: true, strict: false });
(addFormats as unknown as (instance: Ajv) => void)(ajv);
ajv.addFormat("int64", true);

export const validateFcmRequest = ajv.compile(notificationFcmRequestedSchema) as ValidateFunction<
  EventEnvelope<FcmRequestDetail>
>;
export const validateFcmBatch = ajv.compile(fcmBatchSchema) as ValidateFunction<
  EventEnvelope<FcmBatchDetail>
>;
export const validateVerificationEmail = ajv.compile(
  authenticationEmailVerificationRequestedSchema
) as ValidateFunction<EventEnvelope<VerificationEmailDetail>>;
export const validateRecruitingEmail = ajv.compile(
  recruitingInterviewEmailRequestedSchema
) as ValidateFunction<EventEnvelope<RecruitingEmailDetail>>;
export const validateWebhook = ajv.compile(notificationWebhookRequestedSchema) as ValidateFunction<
  EventEnvelope<WebhookDetail>
>;
export const validateEmailResult = ajv.compile(notificationEmailResultSchema) as ValidateFunction<
  EventEnvelope<EmailResultDetail>
>;
export const validateInstallationCommand = ajv.compile(installationCommandSchema);

export function assertValid<T>(validator: ValidateFunction<T>, value: unknown): asserts value is T {
  if (!validator(value)) {
    throw new ContractValidationError(validator.errors ?? []);
  }
}

export class ContractValidationError extends Error {
  constructor(readonly validationErrors: unknown[]) {
    super("notification integration event 계약 검증에 실패했습니다.");
    this.name = "ContractValidationError";
  }
}
