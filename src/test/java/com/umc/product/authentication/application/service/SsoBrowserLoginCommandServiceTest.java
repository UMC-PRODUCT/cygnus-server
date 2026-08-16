package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.event.SsoBrowserLoginCreatedEvent;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByAppleAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByOAuthTokenCommand;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoBrowserLoginCommandService")
class SsoBrowserLoginCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "Strong-Pw-2026";
    private static final String LOGIN_TOKEN = "sso-login-token";

    @Mock
    SsoCredentialVerifier credentialVerifier;

    @Mock
    SsoLoginTokenProvider tokenProvider;

    @Mock
    DomainEventPublisher eventPublisher;

    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;

    @Mock
    VerifyOAuthTokenPort verifyOAuthTokenPort;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    SsoBrowserLoginCommandService service;

    @BeforeEach
    void setUp() {
        SsoProperties properties = new SsoProperties(
            URI.create("https://api.university.neordinary.com"),
            Duration.ofMinutes(3),
            Duration.ofHours(12),
            new SsoProperties.Cookie("UMC_SSO_LOGIN", "", true, "Lax"),
            Map.of()
        );
        service = new SsoBrowserLoginCommandService(
            credentialVerifier,
            tokenProvider,
            properties,
            eventPublisher,
            oAuthAuthenticationUseCase,
            verifyOAuthTokenPort,
            jwtTokenProvider
        );
    }

    @Test
    @DisplayName("email/password 검증 후 SSO 브라우저 로그인 토큰을 발급하고 생성 이벤트를 발행한다")
    void 이메일_로그인_성공_토큰_발급_이벤트_발행() {
        // given
        given(credentialVerifier.verifyEmailPassword(EMAIL, RAW_PASSWORD)).willReturn(MEMBER_ID);
        given(tokenProvider.createLoginToken(any(), any(), any())).willReturn(LOGIN_TOKEN);

        // when
        SsoBrowserLoginInfo result = service.loginByEmail(
            LoginSsoBrowserByEmailCommand.of(EMAIL, RAW_PASSWORD)
        );

        // then
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.loginToken()).isEqualTo(LOGIN_TOKEN);
        assertThat(result.expiresAt()).isAfter(Instant.now().plus(Duration.ofHours(11)));

        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);
        then(tokenProvider).should().createLoginToken(eq(MEMBER_ID), eq("email"), expiresAtCaptor.capture());
        assertThat(expiresAtCaptor.getValue()).isAfter(Instant.now().plus(Duration.ofHours(11)));

        ArgumentCaptor<SsoBrowserLoginCreatedEvent> eventCaptor =
            ArgumentCaptor.forClass(SsoBrowserLoginCreatedEvent.class);
        then(eventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memberId()).isEqualTo(MEMBER_ID);
        assertThat(eventCaptor.getValue().authenticationMethod()).isEqualTo("email");
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("authentication.sso.browser-login.created");
    }

    @Test
    @DisplayName("Kakao OAuth token 기존 회원 로그인은 SSO 브라우저 로그인 토큰을 발급한다")
    void 카카오_OAuth_기존회원_로그인_토큰_발급() {
        // given
        given(oAuthAuthenticationUseCase.accessTokenLogin(any(AccessTokenLoginCommand.class)))
            .willReturn(OAuthTokenLoginResult.existingMember(
                MEMBER_ID,
                OAuthProvider.KAKAO,
                "kakao-provider-id",
                EMAIL
            ));
        given(tokenProvider.createLoginToken(any(), any(), any())).willReturn(LOGIN_TOKEN);

        // when
        SsoBrowserOAuthLoginResult result = service.loginByOAuthToken(
            LoginSsoBrowserByOAuthTokenCommand.of(OAuthProvider.KAKAO, "kakao-id-token")
        );

        // then
        assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.loginToken()).isEqualTo(LOGIN_TOKEN);
        assertThat(result.oAuthVerificationToken()).isNull();

        then(oAuthAuthenticationUseCase).should().accessTokenLogin(AccessTokenLoginCommand.of(
            OAuthProvider.KAKAO,
            "kakao-id-token"
        ));
        then(tokenProvider).should().createLoginToken(eq(MEMBER_ID), eq("kakao"), any());
    }

    @Test
    @DisplayName("Google OAuth token 신규 회원 로그인은 SSO 쿠키 없이 OAuth 가입 토큰을 발급한다")
    void 구글_OAuth_신규회원_가입토큰_발급() {
        // given
        given(oAuthAuthenticationUseCase.accessTokenLogin(any(AccessTokenLoginCommand.class)))
            .willReturn(OAuthTokenLoginResult.newMember(
                OAuthProvider.GOOGLE,
                "google-provider-id",
                EMAIL
            ));
        given(jwtTokenProvider.createOAuthVerificationToken(EMAIL, OAuthProvider.GOOGLE, "google-provider-id"))
            .willReturn("oauth-verification-token");

        // when
        SsoBrowserOAuthLoginResult result = service.loginByOAuthToken(
            LoginSsoBrowserByOAuthTokenCommand.of(OAuthProvider.GOOGLE, "google-id-token")
        );

        // then
        assertThat(result.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.memberId()).isNull();
        assertThat(result.loginToken()).isNull();
        assertThat(result.oAuthVerificationToken()).isEqualTo("oauth-verification-token");
        then(tokenProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Apple authorization code 기존 회원 로그인은 WEB client로 검증하고 refresh token을 갱신한다")
    void 애플_인가코드_기존회원_로그인_refreshToken_갱신() {
        // given
        OAuthAttributes attributes = new OAuthAttributes(OAuthProvider.APPLE, "apple-provider-id", EMAIL);
        given(verifyOAuthTokenPort.verifyAppleAuthorizationCode("apple-code", ClientType.WEB))
            .willReturn(new AppleAuthorizationCodeResult(attributes, "apple-refresh-token", "apple-client-id"));
        given(oAuthAuthenticationUseCase.loginWithOAuthAttributes(attributes))
            .willReturn(OAuthTokenLoginResult.existingMember(
                MEMBER_ID,
                OAuthProvider.APPLE,
                "apple-provider-id",
                EMAIL
            ));
        given(tokenProvider.createLoginToken(any(), any(), any())).willReturn(LOGIN_TOKEN);

        // when
        SsoBrowserOAuthLoginResult result = service.loginByAppleAuthorizationCode(
            LoginSsoBrowserByAppleAuthorizationCodeCommand.from("apple-code")
        );

        // then
        assertThat(result.provider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.loginToken()).isEqualTo(LOGIN_TOKEN);
        then(oAuthAuthenticationUseCase).should().updateAppleRefreshToken(
            OAuthProvider.APPLE,
            "apple-provider-id",
            "apple-refresh-token",
            "apple-client-id"
        );
    }
}
