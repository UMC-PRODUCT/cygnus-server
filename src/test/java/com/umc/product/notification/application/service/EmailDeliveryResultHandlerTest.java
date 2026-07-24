package com.umc.product.notification.application.service;

import static org.mockito.BDDMockito.then;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.service.EmailDeliveryResultHandler.EmailCorrelationType;
import com.umc.product.notification.application.service.EmailDeliveryResultHandler.EmailDeliveryResult;
import com.umc.product.notification.application.service.EmailDeliveryResultHandler.EmailDeliveryStatus;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewMailDeliveryUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailDeliveryResultHandler")
class EmailDeliveryResultHandlerTest {

    @Mock
    ManageRecruitingInterviewMailDeliveryUseCase mailDeliveryUseCase;

    @Test
    @DisplayName("SES 수락 결과는 채용 메일 상태를 SENT로 반영한다")
    void accepted_결과_반영() {
        EmailDeliveryResultHandler handler = new EmailDeliveryResultHandler(mailDeliveryUseCase);
        Instant attemptedAt = Instant.parse("2026-07-23T00:00:00Z");

        handler.handle(new EmailDeliveryResult(
            EmailCorrelationType.RECRUITING_INTERVIEW,
            "40",
            EmailDeliveryStatus.ACCEPTED,
            "ses-message-id",
            null,
            attemptedAt
        ));

        then(mailDeliveryUseCase).should().markRequestMailSent(40L, attemptedAt);
    }

    @Test
    @DisplayName("최종 실패 결과는 provider code만 채용 메일 FAILED 사유로 기록한다")
    void failed_결과_반영() {
        EmailDeliveryResultHandler handler = new EmailDeliveryResultHandler(mailDeliveryUseCase);

        handler.handle(new EmailDeliveryResult(
            EmailCorrelationType.RECRUITING_INTERVIEW,
            "40",
            EmailDeliveryStatus.FAILED,
            null,
            "MessageRejected",
            Instant.parse("2026-07-23T00:00:00Z")
        ));

        then(mailDeliveryUseCase).should().markRequestMailFailed(40L, "MessageRejected");
    }
}
