package com.umc.product.notification.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.notification.application.port.in.ValidateFcmTokensUseCase;
import com.umc.product.notification.application.port.in.dto.FcmTokenValidationInfo;

@DisplayName("FCM 토큰 유효성 검증 스케줄러")
class FcmTokenValidationSchedulerTest {

    @Test
    @DisplayName("FCM 토큰 검증이 활성화되어 있으면 usecase를 호출한다")
    void enabled_호출() {
        FakeValidateFcmTokensUseCase useCase = new FakeValidateFcmTokensUseCase();
        FcmTokenValidationScheduler scheduler = new FcmTokenValidationScheduler(
            new FcmProperties(true, true),
            useCase
        );

        scheduler.validateDueTokens();

        assertThat(useCase.called).isTrue();
    }

    @Test
    @DisplayName("FCM 토큰 검증이 비활성화되어 있으면 usecase를 호출하지 않는다")
    void disabled_스킵() {
        FakeValidateFcmTokensUseCase useCase = new FakeValidateFcmTokensUseCase();
        FcmTokenValidationScheduler scheduler = new FcmTokenValidationScheduler(
            new FcmProperties(true, false),
            useCase
        );

        scheduler.validateDueTokens();

        assertThat(useCase.called).isFalse();
    }

    private static class FakeValidateFcmTokensUseCase implements ValidateFcmTokensUseCase {

        private boolean called;

        @Override
        public FcmTokenValidationInfo validateDueTokens() {
            this.called = true;
            return FcmTokenValidationInfo.of(0, 0, Instant.now());
        }
    }
}
