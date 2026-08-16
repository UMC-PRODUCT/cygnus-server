package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoBrowserLoginQueryService")
class SsoBrowserLoginQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String LOGIN_TOKEN = "sso-login-token";

    @Mock
    SsoLoginTokenProvider tokenProvider;

    @Test
    @DisplayName("유효한 SSO 브라우저 로그인 토큰이면 로그인 정보를 반환한다")
    void 유효한_로그인_토큰_조회_성공() {
        // given
        SsoBrowserLoginQueryService service = new SsoBrowserLoginQueryService(tokenProvider);
        Instant issuedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        given(tokenProvider.parseLoginToken(LOGIN_TOKEN))
            .willReturn(SsoLoginTokenClaims.of(MEMBER_ID, issuedAt, expiresAt, "email"));

        // when
        SsoBrowserLoginInfo result = service.getLogin(LOGIN_TOKEN);

        // then
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.loginToken()).isEqualTo(LOGIN_TOKEN);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("만료된 SSO 브라우저 로그인 토큰이면 INVALID_SSO_BROWSER_LOGIN 예외를 던진다")
    void 만료된_로그인_토큰_거부() {
        // given
        SsoBrowserLoginQueryService service = new SsoBrowserLoginQueryService(tokenProvider);
        Instant issuedAt = Instant.now().minusSeconds(7200);
        Instant expiresAt = Instant.now().minusSeconds(1);
        given(tokenProvider.parseLoginToken(LOGIN_TOKEN))
            .willReturn(SsoLoginTokenClaims.of(MEMBER_ID, issuedAt, expiresAt, "email"));

        // when & then
        assertThatThrownBy(() -> service.getLogin(LOGIN_TOKEN))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
    }
}
