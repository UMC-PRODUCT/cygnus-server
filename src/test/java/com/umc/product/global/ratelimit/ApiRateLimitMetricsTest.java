package com.umc.product.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ApiRateLimitMetricsTest {

    @Test
    @DisplayName("rate limit 메트릭은 client 식별자 없이 낮은 cardinality 태그만 기록한다")
    void record_low_cardinality_metrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiRateLimitMetrics metrics = new ApiRateLimitMetrics(registry);

        metrics.record("blocked", "custom", "GET", "/api/v1/custom/{id}", "WEB");

        assertThat(registry.get("api.rate_limit.requests.total")
            .tag("result", "blocked")
            .tag("rule", "custom")
            .tag("method", "GET")
            .tag("uriTemplate", "/api/v1/custom/{id}")
            .tag("clientType", "WEB")
            .counter()
            .count()).isEqualTo(1);
    }
}
