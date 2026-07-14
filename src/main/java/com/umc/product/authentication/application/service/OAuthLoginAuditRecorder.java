package com.umc.product.authentication.application.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class OAuthLoginAuditRecorder {

    private final RecordAuditLogUseCase recordAuditLogUseCase;

    void recordFailure(String reason) {
        Map<String, Object> details = RecordAuditLogCommand.structuredDetails(
            Map.of(),
            Map.of("type", "OAuthAuthentication"),
            Map.of("reason", reason),
            Map.of(),
            Map.of()
        );
        try {
            recordAuditLogUseCase.record(RecordAuditLogCommand.authenticationFailure(
                AuditAction.LOGIN,
                "OAuthAuthentication",
                null,
                "OAuth 로그인에 실패했습니다.",
                details
            ));
        } catch (RuntimeException auditException) {
            log.warn("OAuth 로그인 실패 감사 기록 호출 실패: errorType={}",
                auditException.getClass().getSimpleName());
        }
    }
}
