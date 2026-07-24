package com.umc.product.notification.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewMailDeliveryUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailDeliveryResultHandler {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final ManageRecruitingInterviewMailDeliveryUseCase recruitingMailDeliveryUseCase;

    public void handle(EmailDeliveryResult result) {
        if (result.correlationType() != EmailCorrelationType.RECRUITING_INTERVIEW) {
            return;
        }
        Long applicationId = parseApplicationId(result.correlationId());
        if (result.status() == EmailDeliveryStatus.ACCEPTED) {
            recruitingMailDeliveryUseCase.markRequestMailSent(applicationId, result.attemptedAt());
            return;
        }
        recruitingMailDeliveryUseCase.markRequestMailFailed(applicationId, failureMessage(result));
    }

    private Long parseApplicationId(String correlationId) {
        try {
            return Long.valueOf(correlationId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("채용 메일 correlationId가 올바르지 않습니다.", exception);
        }
    }

    private String failureMessage(EmailDeliveryResult result) {
        String code = result.failureCode() == null || result.failureCode().isBlank()
            ? result.status().name()
            : result.failureCode();
        return code.substring(0, Math.min(code.length(), MAX_ERROR_LENGTH));
    }

    public record EmailDeliveryResult(
        EmailCorrelationType correlationType,
        String correlationId,
        EmailDeliveryStatus status,
        String providerMessageId,
        String failureCode,
        Instant attemptedAt
    ) {
    }

    public enum EmailCorrelationType {
        EMAIL_VERIFICATION,
        RECRUITING_INTERVIEW
    }

    public enum EmailDeliveryStatus {
        ACCEPTED,
        FAILED,
        EXPIRED
    }
}
