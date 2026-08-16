package com.umc.product.global.logging;

import java.time.Duration;
import java.util.regex.Pattern;

import com.umc.product.global.client.ClientDeviceType;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 운영 관측용 metric recorder.
 *
 * <p>cardinality 폭증을 막기 위해 memberId, fileId, requestId 같은 개별 식별자는 tag 로 받지 않는다.
 */
public class OperationalMetrics {

    private static final String METRIC_EXTERNAL_LATENCY = "operational.external.call.seconds";
    private static final String METRIC_EXTERNAL_TOTAL = "operational.external.call.total";
    private static final String METRIC_BATCH_LATENCY = "operational.batch.job.seconds";
    private static final String METRIC_BATCH_TOTAL = "operational.batch.job.total";
    private static final String METRIC_BATCH_PROCESSED = "operational.batch.job.processed.total";
    private static final String METRIC_NOTIFICATION_TOTAL = "operational.notification.send.total";
    private static final String METRIC_SECURITY_TOTAL = "operational.security.event.total";
    private static final String METRIC_CLIENT_REQUEST_TOTAL = "operational.client.request.total";
    private static final int MAX_TAG_VALUE_LENGTH = 64;
    private static final Pattern HIGH_CARDINALITY_VALUE = Pattern.compile(
        ".*([/?=&@]|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|\\d{6,}).*"
    );

    private final MeterRegistry registry;

    public OperationalMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordExternalCall(String provider, String operation, String result, Duration latency) {
        Timer.builder(METRIC_EXTERNAL_LATENCY)
            .tag("provider", normalize(provider))
            .tag("operation", normalize(operation))
            .tag("result", normalize(result))
            .register(registry)
            .record(nonNegative(latency));
        Counter.builder(METRIC_EXTERNAL_TOTAL)
            .tag("provider", normalize(provider))
            .tag("operation", normalize(operation))
            .tag("result", normalize(result))
            .register(registry)
            .increment();
    }

    public void recordBatchJob(String jobName, String result, Duration duration, long processed) {
        Timer.builder(METRIC_BATCH_LATENCY)
            .tag("jobName", normalize(jobName))
            .tag("result", normalize(result))
            .register(registry)
            .record(nonNegative(duration));
        Counter.builder(METRIC_BATCH_TOTAL)
            .tag("jobName", normalize(jobName))
            .tag("result", normalize(result))
            .register(registry)
            .increment();
        if (processed > 0) {
            Counter.builder(METRIC_BATCH_PROCESSED)
                .tag("jobName", normalize(jobName))
                .tag("result", normalize(result))
                .register(registry)
                .increment(processed);
        }
    }

    public void recordNotification(String provider, String operation, String result, long count) {
        if (count <= 0) {
            return;
        }
        Counter.builder(METRIC_NOTIFICATION_TOTAL)
            .tag("provider", normalize(provider))
            .tag("operation", normalize(operation))
            .tag("result", normalize(result))
            .register(registry)
            .increment(count);
    }

    public void recordSecurityEvent(String domain, String operation, String result) {
        Counter.builder(METRIC_SECURITY_TOTAL)
            .tag("domain", normalize(domain))
            .tag("operation", normalize(operation))
            .tag("result", normalize(result))
            .register(registry)
            .increment();
    }

    public void recordClientRequest(
        ClientServiceType serviceType,
        ClientDeviceType deviceType,
        ClientEnvironment environment,
        String source,
        String statusFamily
    ) {
        Counter.builder(METRIC_CLIENT_REQUEST_TOTAL)
            .tag("service", normalize(enumName(serviceType)))
            .tag("device", normalize(enumName(deviceType)))
            .tag("environment", normalize(enumName(environment)))
            .tag("source", normalize(source))
            .tag("statusFamily", normalize(statusFamily))
            .register(registry)
            .increment();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TAG_VALUE_LENGTH || HIGH_CARDINALITY_VALUE.matcher(normalized).matches()) {
            return "other";
        }
        return normalized;
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static Duration nonNegative(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }
}
