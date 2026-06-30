package com.umc.product.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("FCM 토큰")
class FcmTokenTest {

    @Test
    @DisplayName("생성 시 등록 시각과 검증 시각을 함께 초기화한다")
    void create_등록시각과_검증시각_초기화() {
        FcmToken token = FcmToken.create(1L, "token", "ios", "device", "1.0.0");

        assertThat(token.isActive()).isTrue();
        assertThat(token.getLastRegisteredAt()).isNotNull();
        assertThat(token.getLastValidatedAt()).isEqualTo(token.getLastRegisteredAt());
    }

    @Test
    @DisplayName("재등록 시 등록 시각과 검증 시각을 갱신하고 다시 활성화한다")
    void register_등록시각과_검증시각_갱신() {
        FcmToken token = FcmToken.create(1L, "token");
        token.deactivate();
        ReflectionTestUtils.setField(token, "lastRegisteredAt", Instant.EPOCH);
        ReflectionTestUtils.setField(token, "lastValidatedAt", Instant.EPOCH);

        token.register("android", "device", "1.0.1");

        assertThat(token.isActive()).isTrue();
        assertThat(token.getDeactivatedAt()).isNull();
        assertThat(token.getLastRegisteredAt()).isAfter(Instant.EPOCH);
        assertThat(token.getLastValidatedAt()).isEqualTo(token.getLastRegisteredAt());
    }
}
