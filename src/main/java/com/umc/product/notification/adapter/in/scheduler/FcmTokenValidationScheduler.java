package com.umc.product.notification.adapter.in.scheduler;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.notification.application.port.in.ValidateFcmTokensUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmTokenValidationScheduler {

    private final FcmProperties fcmProperties;
    private final ValidateFcmTokensUseCase validateFcmTokensUseCase;

    @Scheduled(fixedRateString = "${app.fcm.token-validation-interval-ms}")
    public void validateDueTokens() {
        if (!fcmProperties.enabled() || !fcmProperties.tokenValidationEnabled()) {
            return;
        }
        log.debug("FCM 토큰 유효성 검증 스케줄 실행");
        validateFcmTokensUseCase.validateDueTokens();
    }
}
