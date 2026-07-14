package com.umc.product.audit.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.logging.AuditRequestContextProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecordAuditLogService implements RecordAuditLogUseCase {

    private final AuditLogNewTransactionWriter newTransactionWriter;
    private final AuditLogRecordingMonitor recordingMonitor;
    private final DomainEventPublisher eventPublisher;
    private final AuditRequestContextProvider requestContextProvider;

    @Override
    public void record(RecordAuditLogCommand command) {
        Objects.requireNonNull(command, "감사 로그 command는 필수입니다.");
        RecordAuditLogCommand enriched;
        try {
            enriched = enrichContext(command, requestContextProvider.capture());
        } catch (Exception exception) {
            recordingMonitor.onProcessingFailure(
                command.domain(),
                command.action(),
                "context_extraction",
                exception
            );
            return;
        }
        if (enriched.outcome() == AuditOutcome.SUCCESS) {
            try {
                eventPublisher.publish(toEvent(enriched));
            } catch (Exception exception) {
                recordingMonitor.onProcessingFailure(
                    enriched.domain(),
                    enriched.action(),
                    "event_publish",
                    exception
                );
            }
            return;
        }
        try {
            newTransactionWriter.recordFailure(enriched);
            recordingMonitor.onSuccess(enriched);
        } catch (Exception exception) {
            recordingMonitor.onFailure(enriched, exception);
        }
    }

    private RecordAuditLogCommand enrichContext(
        RecordAuditLogCommand command,
        AuditRequestContextProvider.Snapshot context
    ) {
        return RecordAuditLogCommand.of(
            command.domain(),
            command.action(),
            command.targetType(),
            command.targetId(),
            command.actorMemberId(),
            command.description(),
            command.details(),
            command.ipAddress() != null ? command.ipAddress() : context.ipAddress(),
            command.outcome(),
            command.source(),
            command.requestId() != null ? command.requestId() : context.requestId(),
            command.traceId() != null ? command.traceId() : context.traceId()
        );
    }

    private AuditLogEvent toEvent(RecordAuditLogCommand command) {
        return AuditLogEvent.builder()
            .domain(command.domain())
            .action(command.action())
            .targetType(command.targetType())
            .targetId(command.targetId())
            .actorMemberId(command.actorMemberId())
            .description(command.description())
            .details(command.details())
            .ipAddress(command.ipAddress())
            .outcome(command.outcome())
            .source(command.source())
            .requestId(command.requestId())
            .traceId(command.traceId())
            .build();
    }
}
