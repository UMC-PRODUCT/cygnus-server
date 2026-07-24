import { randomUUID } from "node:crypto";
import { PutEventsCommand } from "@aws-sdk/client-eventbridge";
import type { EmailResultDetail, EventEnvelope } from "../contracts/types.js";
import { EVENT_TYPES } from "../contracts/types.js";
import { assertValid, validateEmailResult } from "../contracts/schemas.js";
import { eventBridge } from "../shared/aws.js";
import { config } from "../shared/config.js";

export async function publishEmailResult(
  request: EventEnvelope<unknown>,
  detail: EmailResultDetail
): Promise<void> {
  const eventType = detail.status === "ACCEPTED"
    ? EVENT_TYPES.emailAccepted
    : EVENT_TYPES.emailFailed;
  const envelope: EventEnvelope<EmailResultDetail> = {
    schemaVersion: 1,
    eventId: randomUUID(),
    eventType,
    source: "umc-product.notification",
    occurredAt: new Date().toISOString(),
    ...(request.traceparent !== undefined ? { traceparent: request.traceparent } : {}),
    requestId: request.requestId,
    detail
  };
  assertValid(validateEmailResult, envelope);
  const response = await eventBridge.send(new PutEventsCommand({
    Entries: [{
      EventBusName: config.eventBusName,
      Source: envelope.source,
      DetailType: eventType,
      Time: new Date(envelope.occurredAt),
      Detail: JSON.stringify(envelope)
    }]
  }));
  if ((response.FailedEntryCount ?? 0) > 0) {
    throw new Error(`Email delivery result EventBridge 발행 실패: ${response.Entries?.[0]?.ErrorCode ?? "UNKNOWN"}`);
  }
}
