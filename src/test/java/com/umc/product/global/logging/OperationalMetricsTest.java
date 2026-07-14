package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

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
    @DisplayName("감사 failure counter는 Prometheus scrape 이름과 저카디널리티 태그를 유지한다")
    void audit_failure_counter_prometheus_scrape() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        OperationalMetrics metrics = new OperationalMetrics(registry);

        metrics.recordAuditLog("MEMBER", "UPDATE", "FAILURE", "SYSTEM");
        metrics.recordAuditLogFailure("MEMBER", "UPDATE", new IllegalStateException("target-123456"));

        String scrape = registry.scrape();

        assertThat(scrape)
            .contains("operational_audit_log_total")
            .contains("operational_audit_log_failure_total")
            .contains("action=\"UPDATE\"")
            .contains("domain=\"MEMBER\"")
            .contains("outcome=\"FAILURE\"")
            .contains("source=\"SYSTEM\"")
            .contains("reason=\"IllegalStateException\"")
            .doesNotContain("targetId", "requestId", "traceId", "target-123456");
    }
}
