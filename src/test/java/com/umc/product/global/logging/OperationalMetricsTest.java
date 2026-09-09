package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OperationalMetricsTest {

    @Test
    @DisplayName("메트릭 태그 값에 높은 cardinality 값이 들어오면 other로 축약한다")
    void collapse_high_cardinality_tag_values() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordNotification("FCM", "member-123456789", "success", 1);
        metrics.recordExternalCall("https://example.com/callback?id=123", "CALL", "success", Duration.ZERO);

        assertThat(registry.get("operational.notification.send.total")
            .tag("provider", "FCM")
            .tag("operation", "other")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("operational.external.call.total")
            .tag("provider", "other")
            .tag("operation", "CALL")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(1);
    }
}
