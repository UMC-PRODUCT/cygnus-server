package com.umc.product.audit.application.service.command;

import org.springframework.stereotype.Component;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.logging.OperationalMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRecordingMonitor {

    private final OperationalMetrics operationalMetrics;

    public void onSuccess(AuditLogEvent event) {
        recordTotal(event.domain().name(), event.action().name(), event.outcome().name(), event.source().name());
    }

    public void onSuccess(RecordAuditLogCommand command) {
        recordTotal(command.domain().name(), command.action().name(), command.outcome().name(), command.source().name());
    }

    public void onFailure(AuditLogEvent event, Exception exception) {
        recordFailure(event.domain().name(), event.action().name(), event.outcome().name(), event.source().name(), exception);
        log.error(
            "감사 로그 저장 실패: domain={}, action={}, target={}:{}, outcome={}, source={}, error={}",
            event.domain(),
            event.action(),
            event.targetType(),
            event.targetId(),
            event.outcome(),
            event.source(),
            exception.getMessage(),
            exception
        );
    }

    public void onFailure(RecordAuditLogCommand command, Exception exception) {
        recordFailure(command.domain().name(), command.action().name(), command.outcome().name(), command.source().name(), exception);
        log.error(
            "감사 로그 저장 실패: domain={}, action={}, target={}:{}, outcome={}, source={}, error={}",
            command.domain(),
            command.action(),
            command.targetType(),
            command.targetId(),
            command.outcome(),
            command.source(),
            exception.getMessage(),
            exception
        );
    }

    public void onProcessingFailure(
        AuditLogEvent event,
        String reason,
        Exception exception
    ) {
        recordProcessingFailure(event.domain(), event.action(), reason, exception);
    }

    public void onProcessingFailure(
        Domain domain,
        AuditAction action,
        String reason,
        Exception exception
    ) {
        recordProcessingFailure(domain, action, reason, exception);
    }

    private void recordTotal(String domain, String action, String outcome, String source) {
        try {
            operationalMetrics.recordAuditLog(domain, action, outcome, source);
        } catch (RuntimeException metricsException) {
            log.warn("감사 로그 metric 기록 실패: errorType={}", metricsException.getClass().getSimpleName());
        }
    }

    private void recordFailure(
        String domain,
        String action,
        String outcome,
        String source,
        Exception exception
    ) {
        try {
            operationalMetrics.recordAuditLog(domain, action, outcome, source);
            operationalMetrics.recordAuditLogFailure(domain, action, exception);
        } catch (RuntimeException metricsException) {
            log.warn("감사 로그 failure metric 기록 실패: errorType={}", metricsException.getClass().getSimpleName());
        }
    }

    private void recordProcessingFailure(
        Domain domain,
        AuditAction action,
        String reason,
        Exception exception
    ) {
        try {
            operationalMetrics.recordAuditLogFailure(domain.name(), action.name(), reason);
        } catch (RuntimeException metricsException) {
            log.warn("감사 처리 failure metric 기록 실패: errorType={}",
                metricsException.getClass().getSimpleName());
        }
        log.error(
            "감사 로그 처리 실패: domain={}, action={}, reason={}, errorType={}",
            domain,
            action,
            reason,
            exception.getClass().getSimpleName(),
            exception
        );
    }
}
