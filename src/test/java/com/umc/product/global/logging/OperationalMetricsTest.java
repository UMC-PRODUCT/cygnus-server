package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.client.ClientDeviceType;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OperationalMetricsTest {

    @Test
    @DisplayName("외부 호출 메트릭은 provider operation result 낮은 cardinality 태그로 기록한다")
    void record_external_call_metrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordExternalCall("KAKAO", "VERIFY_ID_TOKEN", "success", Duration.ofMillis(25));

        assertThat(registry.get("operational.external.call.total")
            .tag("provider", "KAKAO")
            .tag("operation", "VERIFY_ID_TOKEN")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("operational.external.call.seconds")
            .tag("provider", "KAKAO")
            .tag("operation", "VERIFY_ID_TOKEN")
            .tag("result", "success")
            .timer()
            .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("배치와 알림과 보안 이벤트 메트릭은 집계 가능한 값만 기록한다")
    void record_operational_metrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordBatchJob("email_verification_retention", "success", Duration.ofMillis(10), 7);
        metrics.recordNotification("FCM", "SEND_TO_MEMBERS", "success", 3);
        metrics.recordSecurityEvent("AUTHORIZATION", "ACCESS_DENIED", "denied");

        assertThat(registry.get("operational.batch.job.total")
            .tag("jobName", "email_verification_retention")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("operational.batch.job.processed.total")
            .tag("jobName", "email_verification_retention")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(7);
        assertThat(registry.get("operational.notification.send.total")
            .tag("provider", "FCM")
            .tag("operation", "SEND_TO_MEMBERS")
            .tag("result", "success")
            .counter()
            .count()).isEqualTo(3);
        assertThat(registry.get("operational.security.event.total")
            .tag("domain", "AUTHORIZATION")
            .tag("operation", "ACCESS_DENIED")
            .tag("result", "denied")
            .counter()
            .count()).isEqualTo(1);
    }

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

    @Test
    @DisplayName("0건 알림은 무시하고 null·blank·긴 tag 및 음수 duration을 안전한 값으로 정규화한다")
    void normalize_metric_edge_values() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordNotification("FCM", "SEND", "success", 0);
        metrics.recordExternalCall(null, " ", "x".repeat(65), null);
        metrics.recordBatchJob("job", "failure", Duration.ofSeconds(-1), 0);
        metrics.recordClientRequest(null, null, null, null, " ");

        assertThat(registry.find("operational.notification.send.total").counter()).isNull();
        assertThat(registry.get("operational.external.call.total")
            .tag("provider", "unknown")
            .tag("operation", "unknown")
            .tag("result", "other")
            .counter()).isNotNull();
        assertThat(registry.get("operational.batch.job.seconds").timer().totalTime(
            java.util.concurrent.TimeUnit.NANOSECONDS
        )).isZero();
        assertThat(registry.get("operational.client.request.total")
            .tag("service", "unknown")
            .tag("device", "unknown")
            .tag("environment", "unknown")
            .tag("source", "unknown")
            .tag("statusFamily", "unknown")
            .counter()).isNotNull();
    }

    @Test
    @DisplayName("client request enum은 enum 이름으로 기록한다")
    void client_request_enum_names() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordClientRequest(
            ClientServiceType.UMC_WEBSITE,
            ClientDeviceType.DESKTOP,
            ClientEnvironment.PROD,
            "origin",
            "2xx"
        );

        assertThat(registry.get("operational.client.request.total")
            .tag("service", "UMC_WEBSITE")
            .counter()).isNotNull();
    }
}
