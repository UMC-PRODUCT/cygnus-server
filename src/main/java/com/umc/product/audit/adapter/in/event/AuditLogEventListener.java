package com.umc.product.audit.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.application.service.command.AuditLogRecordingMonitor;
import com.umc.product.audit.domain.AuditLogEvent;

import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 이벤트를 비동기로 수신하여 저장합니다.
 * <p>
 * 비즈니스 트랜잭션 커밋 후에만 저장하여 롤백된 트랜잭션의 유령 로그를 방지합니다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final SaveAuditLogUseCase saveAuditLogUseCase;
    private final AuditLogRecordingMonitor recordingMonitor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(AuditLogEvent event) {
        try {
            saveAuditLogUseCase.save(event);
            recordingMonitor.onSuccess(event);
        } catch (RuntimeException exception) {
            recordingMonitor.onFailure(event, exception);
            throw exception;
        }
    }
}
