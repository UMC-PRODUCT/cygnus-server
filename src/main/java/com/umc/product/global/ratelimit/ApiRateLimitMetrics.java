package com.umc.product.global.ratelimit;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ApiRateLimitMetrics {

    private static final String METRIC_RATE_LIMIT_REQUESTS = "api.rate_limit.requests.total";
    private static final int MAX_TAG_VALUE_LENGTH = 128;
    private static final Pattern HIGH_CARDINALITY_URI = Pattern.compile(
        ".*([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|\\d{6,}).*"
    );

    private final MeterRegistry registry;

    public ApiRateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String result, String rule, String method, String uriTemplate, String clientType) {
        Counter.builder(METRIC_RATE_LIMIT_REQUESTS)
            .tag("result", normalize(result))
            .tag("rule", normalize(rule))
            .tag("method", normalize(method))
            .tag("uriTemplate", normalizeUriTemplate(uriTemplate))
            .tag("clientType", normalize(clientType))
            .register(registry)
            .increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TAG_VALUE_LENGTH) {
            return "other";
        }
        return normalized;
    }

    private String normalizeUriTemplate(String uriTemplate) {
        String normalized = normalize(uriTemplate);
        if (HIGH_CARDINALITY_URI.matcher(normalized).matches()) {
            return "other";
        }
        return normalized;
    }
}
