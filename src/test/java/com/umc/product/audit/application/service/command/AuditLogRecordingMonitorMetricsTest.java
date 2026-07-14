package com.umc.product.audit.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.audit.adapter.in.event.AuditLogEventListener;
import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.logging.OperationalMetrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("감사 로그 운영 metric 기록")
class AuditLogRecordingMonitorMetricsTest {

    private static final String TOTAL_METRIC = "operational.audit.log.total";
    private static final String FAILURE_METRIC = "operational.audit.log.failure.total";
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AuditLogRecordingMonitor monitor = new AuditLogRecordingMonitor(
        new OperationalMetrics(registry)
    );

    @Test
    @DisplayName("이벤트 listener와 직접 recorder의 성공·실패를 저카디널리티 counter에 누적한다")
    void 이벤트_listener와_직접_recorder의_성공_실패를_counter에_누적한다() {
        // given
        AuditLogEventListener successListener = new AuditLogEventListener(event -> {
        }, monitor);
        AuditLogEventListener failureListener = new AuditLogEventListener(failingSavePort(), monitor);
        IllegalStateException failure = new IllegalStateException("save failed");

        // when
        successListener.handle(event(AuditOutcome.SUCCESS, AuditSource.ANNOTATION, "event-success"));
        assertThatThrownBy(() ->
            failureListener.handle(event(AuditOutcome.FAILURE, AuditSource.SYSTEM, "event-failure"))
        ).isInstanceOf(IllegalStateException.class);
        monitor.onSuccess(command(AuditOutcome.SUCCESS, "direct-success"));
        monitor.onFailure(command(AuditOutcome.FAILURE, "direct-failure-1"), failure);
        monitor.onFailure(command(AuditOutcome.FAILURE, "direct-failure-2"), failure);

        // then
        assertCounter(TOTAL_METRIC, 1D,
            "domain", "MEMBER", "action", "UPDATE", "outcome", "SUCCESS", "source", "ANNOTATION");
        assertCounter(TOTAL_METRIC, 1D,
            "domain", "MEMBER", "action", "UPDATE", "outcome", "SUCCESS", "source", "SYSTEM");
        assertCounter(TOTAL_METRIC, 3D,
            "domain", "MEMBER", "action", "UPDATE", "outcome", "FAILURE", "source", "SYSTEM");
        assertCounter(FAILURE_METRIC, 3D,
            "domain", "MEMBER", "action", "UPDATE", "reason", "IllegalStateException");
        assertThat(registry.getMeters().stream()
            .filter(meter -> meter.getId().getName().startsWith("operational.audit.log"))
            .flatMap(meter -> meter.getId().getTags().stream())
            .map(Tag::getKey))
            .doesNotContain("targetId", "requestId", "traceId");
    }

    private void assertCounter(String metric, double expected, String... tags) {
        assertThat(registry.get(metric).tags(tags).counter().count()).isEqualTo(expected);
    }

    private static SaveAuditLogUseCase failingSavePort() {
        return event -> {
            throw new IllegalStateException("save failed for " + event.targetId());
        };
    }

    private static AuditLogEvent event(AuditOutcome outcome, AuditSource source, String targetId) {
        return AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId(targetId)
            .outcome(outcome)
            .source(source)
            .requestId("request-" + targetId)
            .traceId("trace-" + targetId)
            .build();
    }

    private static RecordAuditLogCommand command(AuditOutcome outcome, String targetId) {
        return RecordAuditLogCommand.of(
            Domain.MEMBER,
            AuditAction.UPDATE,
            "Member",
            targetId,
            7L,
            "운영 metric probe",
            Map.of(),
            "198.51.100.7",
            outcome,
            AuditSource.SYSTEM,
            "request-" + targetId,
            "trace-" + targetId
        );
    }
}
