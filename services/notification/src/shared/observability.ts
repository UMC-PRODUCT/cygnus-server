import { Logger } from "@aws-lambda-powertools/logger";
import { Metrics, MetricUnit } from "@aws-lambda-powertools/metrics";
import { Tracer } from "@aws-lambda-powertools/tracer";

export const logger = new Logger({ serviceName: "umc-product-notification" });
export const metrics = new Metrics({
  namespace: "UMCProduct/Notification",
  serviceName: "umc-product-notification"
});
export const tracer = new Tracer({ serviceName: "umc-product-notification" });

export function safeErrorName(error: unknown): string {
  if (typeof error === "object" && error !== null && "name" in error
    && typeof error.name === "string") {
    return error.name.slice(0, 100);
  }
  return typeof error;
}

export function recordDelivery(
  channel: "FCM" | "EMAIL" | "WEBHOOK",
  outcome: "success" | "failure" | "retry" | "invalid" | "expired",
  value = 1
): void {
  metrics.addDimension("Channel", channel);
  const metricName = `Delivery${outcome[0]?.toUpperCase() ?? ""}${outcome.slice(1)}Count`;
  metrics.addMetric(metricName, MetricUnit.Count, value);
}
