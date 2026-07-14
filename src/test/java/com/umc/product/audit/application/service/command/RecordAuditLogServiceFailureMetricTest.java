package com.umc.product.audit.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.logging.AuditRequestContextProvider;
import com.umc.product.global.logging.OperationalMetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("명시 감사 recorder 처리 실패 metric")
class RecordAuditLogServiceFailureMetricTest {

    private static final String FAILURE_METRIC = "operational.audit.log.failure.total";

    private final AuditLogNewTransactionWriter writer = mock(AuditLogNewTransactionWriter.class);
    private final DomainEventPublisher publisher = mock(DomainEventPublisher.class);
    private final AuditRequestContextProvider contextProvider = mock(AuditRequestContextProvider.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private RecordAuditLogService service;

    @BeforeEach
    void setUp() {
        AuditLogRecordingMonitor monitor = new AuditLogRecordingMonitor(new OperationalMetrics(registry));
        service = new RecordAuditLogService(writer, monitor, publisher, contextProvider);
        when(contextProvider.capture()).thenReturn(new AuditRequestContextProvider.Snapshot(null, null, null));
    }

    @Test
    @DisplayName("SUCCESS 이벤트 발행 실패는 event_publish reason으로 기록한다")
    void eventPublishFailureUsesFixedReason() {
        doThrow(new IllegalStateException("publisher failed")).when(publisher).publish(any());

        service.record(successCommand());

        assertThat(failureCount("event_publish")).isEqualTo(1D);
    }

    @Test
    @DisplayName("request context 추출 실패는 context_extraction reason으로 기록한다")
    void contextExtractionFailureUsesFixedReason() {
        when(contextProvider.capture()).thenThrow(new IllegalStateException("context failed"));

        service.record(successCommand());

        assertThat(failureCount("context_extraction")).isEqualTo(1D);
    }

    private RecordAuditLogCommand successCommand() {
        return RecordAuditLogCommand.success(
            Domain.AUDIT_LOG,
            AuditAction.CREATE,
            "MetricProbe",
            "fixed-reason",
            null,
            "감사 metric probe를 기록했습니다.",
            Map.of()
        );
    }

    private double failureCount(String reason) {
        Counter counter = registry.find(FAILURE_METRIC)
            .tags("domain", "AUDIT_LOG", "action", "CREATE", "reason", reason)
            .counter();
        return counter == null ? 0D : counter.count();
    }
}
