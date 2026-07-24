import type { SQSRecord } from "aws-lambda";
import type { EventBridgeSqsMessage } from "../contracts/types.js";

export function parseSqsBody(record: SQSRecord): unknown {
  const parsed = JSON.parse(record.body) as unknown;
  if (isEventBridgeMessage(parsed)) {
    return parsed.detail;
  }
  return parsed;
}

function isEventBridgeMessage(value: unknown): value is EventBridgeSqsMessage {
  return typeof value === "object"
    && value !== null
    && "detail-type" in value
    && "detail" in value;
}

export function receiveCount(record: SQSRecord): number {
  const raw = record.attributes.ApproximateReceiveCount;
  const value = Number.parseInt(raw, 10);
  return Number.isFinite(value) ? value : 1;
}

export function partition<T>(values: T[], size: number): T[][] {
  const result: T[][] = [];
  for (let index = 0; index < values.length; index += size) {
    result.push(values.slice(index, index + size));
  }
  return result;
}
