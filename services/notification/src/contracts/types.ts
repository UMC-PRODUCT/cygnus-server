import type {
  EmailResultDetail,
  FcmRequestDetail,
  RecruitingEmailDetail,
  VerificationEmailDetail,
  WebhookDetail
} from "./generated.js";

export type {
  EmailResultDetail,
  FcmRequestDetail,
  RecruitingEmailDetail,
  VerificationEmailDetail,
  WebhookDetail
};

export const EVENT_TYPES = {
  fcmRequested: "notification.fcm.requested.v1",
  verificationEmailRequested: "authentication.email.verification.requested.v1",
  recruitingEmailRequested: "recruiting.interview.email.requested.v1",
  webhookRequested: "notification.webhook.requested.v1",
  emailAccepted: "notification.email.accepted.v1",
  emailFailed: "notification.email.failed.v1"
} as const;

export type EventType = (typeof EVENT_TYPES)[keyof typeof EVENT_TYPES];

export interface EventEnvelope<TDetail> {
  schemaVersion: 1;
  eventId: string;
  eventType: EventType | "notification.fcm.batch.requested.v1";
  source: string;
  occurredAt: string;
  traceparent?: string | null;
  requestId: string;
  detail: TDetail;
}

export interface FcmBatchDetail {
  batchId: string;
  installationIds: string[];
  title: string;
  body: string;
  data: Record<string, string>;
  imageUrl?: string | null;
  deepLink?: string | null;
  attempt: number;
}

export type WebhookPlatform = WebhookDetail["platforms"][number];

export interface FcmInstallationCommand {
  commandId: string;
  action: "UPSERT" | "DEACTIVATE";
  occurredAt: string;
  memberId: number;
  installationId: string;
  fcmToken?: string;
  platform?: string | null;
  appVersion?: string | null;
}

export interface EventBridgeSqsMessage {
  version: string;
  id: string;
  "detail-type": string;
  source: string;
  time: string;
  detail: unknown;
}
