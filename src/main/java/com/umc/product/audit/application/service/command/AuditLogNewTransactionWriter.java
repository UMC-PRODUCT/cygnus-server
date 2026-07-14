package com.umc.product.audit.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogNewTransactionWriter {

    private final SaveAuditLogUseCase saveAuditLogUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(RecordAuditLogCommand command) {
        if (command.outcome() != AuditOutcome.FAILURE) {
            throw new IllegalArgumentException("독립 트랜잭션은 FAILURE 감사에만 사용할 수 있습니다.");
        }
        AuditLogEvent event = AuditLogEvent.builder()
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

        saveAuditLogUseCase.save(event);
    }
}
