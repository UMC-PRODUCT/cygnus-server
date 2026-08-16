package com.umc.product.authentication.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

class RefreshTokenTest {

    private static final Long MEMBER_ID = 1L;
    private static final UUID JTI = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("소유자와 만료시각이 유효하면 검증을 통과한다")
    void 활성_refresh_token_검증_성공() {
        // given
        RefreshToken refreshToken = RefreshToken.create(JTI, MEMBER_ID, Instant.now().plusSeconds(60));

        // when / then
        assertThatCode(() -> refreshToken.validateActiveFor(MEMBER_ID))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("legacy RefreshToken은 stored clientId와 claim clientId가 모두 없으면 검증을 통과한다")
    void legacy_client_id_검증_성공() {
        // given
        RefreshToken refreshToken = RefreshToken.create(JTI, MEMBER_ID, Instant.now().plusSeconds(60));

        // when / then
        assertThatCode(() -> refreshToken.validateActiveFor(MEMBER_ID, null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSO RefreshToken은 stored clientId와 claim clientId가 같으면 검증을 통과한다")
    void sso_client_id_검증_성공() {
        // given
        RefreshToken refreshToken = RefreshToken.create(
            JTI,
            MEMBER_ID,
            Instant.now().plusSeconds(60),
            "backoffice"
        );

        // when / then
        assertThatCode(() -> refreshToken.validateActiveFor(MEMBER_ID, "backoffice"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("stored clientId와 claim clientId 중 하나만 있거나 서로 다르면 INVALID_REFRESH_TOKEN 예외를 던진다")
    void client_id_mismatch_거부() {
        // given
        RefreshToken legacyStored = RefreshToken.create(JTI, MEMBER_ID, Instant.now().plusSeconds(60));
        RefreshToken ssoStored = RefreshToken.create(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            MEMBER_ID,
            Instant.now().plusSeconds(60),
            "backoffice"
        );

        // when / then
        assertThatThrownBy(() -> legacyStored.validateActiveFor(MEMBER_ID, "backoffice"))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);

        assertThatThrownBy(() -> ssoStored.validateActiveFor(MEMBER_ID, null))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);

        assertThatThrownBy(() -> ssoStored.validateActiveFor(MEMBER_ID, "website"))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("다른 회원의 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다")
    void 다른_회원_refresh_token_거부() {
        // given
        RefreshToken refreshToken = RefreshToken.create(JTI, MEMBER_ID, Instant.now().plusSeconds(60));

        // when / then
        assertThatThrownBy(() -> refreshToken.validateActiveFor(2L))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("만료된 RefreshToken이면 EXPIRED_JWT_TOKEN 예외를 던진다")
    void 만료된_refresh_token_거부() {
        // given
        RefreshToken refreshToken = RefreshToken.create(JTI, MEMBER_ID, Instant.now().minusSeconds(1));

        // when / then
        assertThatThrownBy(() -> refreshToken.validateActiveFor(MEMBER_ID))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
    }
}
