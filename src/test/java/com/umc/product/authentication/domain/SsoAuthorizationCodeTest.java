package com.umc.product.authentication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;

@DisplayName("SsoAuthorizationCode")
class SsoAuthorizationCodeTest {

    private static final String CODE_HASH = "hash-code";
    private static final Long MEMBER_ID = 1L;
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://backoffice.university.neordinary.com/auth/callback";
    private static final String CODE_CHALLENGE = "challenge";
    private static final Instant NOW = Instant.parse("2026-06-26T00:00:00Z");

    @Test
    @DisplayName("authorization code는 client와 redirect URI가 일치할 때 한 번만 소비된다")
    void authorization_code_소비_성공_후_재사용_실패() {
        // given
        SsoAuthorizationCode code = createCode(NOW.plusSeconds(180));

        // when
        code.consume(CLIENT_ID, REDIRECT_URI, NOW.plusSeconds(30));

        // then
        assertThat(code.getUsedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThatThrownBy(() -> code.consume(CLIENT_ID, REDIRECT_URI, NOW.plusSeconds(40)))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("authorization code가 만료되면 소비할 수 없다")
    void authorization_code_만료_시_소비_실패() {
        // given
        SsoAuthorizationCode code = createCode(NOW.plusSeconds(180));

        // when & then
        assertThatThrownBy(() -> code.consume(CLIENT_ID, REDIRECT_URI, NOW.plusSeconds(180)))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("authorization code는 요청 client가 다르면 소비할 수 없다")
    void authorization_code_client_불일치_시_소비_실패() {
        // given
        SsoAuthorizationCode code = createCode(NOW.plusSeconds(180));

        // when & then
        assertThatThrownBy(() -> code.consume("another-client", REDIRECT_URI, NOW.plusSeconds(30)))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("authorization code는 요청 redirect URI가 다르면 소비할 수 없다")
    void authorization_code_redirect_uri_불일치_시_소비_실패() {
        // given
        SsoAuthorizationCode code = createCode(NOW.plusSeconds(180));

        // when & then
        assertThatThrownBy(() -> code.consume(CLIENT_ID, "https://example.com/callback", NOW.plusSeconds(30)))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("authorization code 생성 필수값이 비어 있으면 실패한다")
    void authorization_code_생성_필수값_검증() {
        // when & then
        assertThatThrownBy(() -> SsoAuthorizationCode.create(
            "",
            MEMBER_ID,
            CLIENT_ID,
            REDIRECT_URI,
            CODE_CHALLENGE,
            PkceChallengeMethod.S256,
            NOW.plusSeconds(180)
        )).isInstanceOf(AuthenticationDomainException.class);
    }

    private SsoAuthorizationCode createCode(Instant expiresAt) {
        return SsoAuthorizationCode.create(
            CODE_HASH,
            MEMBER_ID,
            CLIENT_ID,
            REDIRECT_URI,
            CODE_CHALLENGE,
            PkceChallengeMethod.S256,
            expiresAt
        );
    }
}
