package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authentication.application.service.SsoLoginTokenClaims;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JwtTokenProvider 의 emailVerificationToken purpose claim 검증 단위 테스트.
 * <p>
 * 회원가입(REGISTER) 흐름에서 발급된 토큰이 비밀번호 초기화(PASSWORD_RESET) 나 이메일 변경(CHANGE_EMAIL) 에 재사용되지
 * 않도록, 발급 시 purpose 를 claim 으로 심고 파싱 시 expectedPurpose 와 일치해야만
 * 통과시키는 동작을 검증한다.
 */
class JwtTokenProviderEmailVerificationTest {

    private static final String EMAIL = "alice@example.com";
    private static final String ACCESS_TOKEN_SECRET = "test-access-token-secret-must-be-long-enough-for-hmac-sha256";
    private static final String REFRESH_TOKEN_SECRET = "test-refresh-token-secret-must-be-long-enough-for-hmac-sha256";
    private static final String OAUTH_VERIFICATION_TOKEN_SECRET =
        "test-oauth-verification-token-secret-must-be-long-enough-for-hmac-sha256";
    private static final String EMAIL_VERIFICATION_TOKEN_SECRET =
        "test-email-verification-token-secret-must-be-long-enough-for-hmac-sha256";
    private static final String SSO_LOGIN_TOKEN_SECRET =
        "test-sso-login-token-secret-must-be-long-enough-for-hmac-sha256";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
            ACCESS_TOKEN_SECRET,
            REFRESH_TOKEN_SECRET,
            OAUTH_VERIFICATION_TOKEN_SECRET,
            EMAIL_VERIFICATION_TOKEN_SECRET,
            SSO_LOGIN_TOKEN_SECRET,
            3600L,
            3600L,
            600L
        );
    }

    @Test
    @DisplayName("REGISTER 로 발급한 토큰은 REGISTER 로 파싱 시 이메일을 반환한다")
    void purpose_일치_REGISTER_정상_파싱() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.REGISTER);

        // when
        String parsedEmail = provider.parseEmailVerificationToken(token, EmailVerificationPurpose.REGISTER);

        // then
        assertThat(parsedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("PASSWORD_RESET 로 발급한 토큰은 PASSWORD_RESET 로 파싱 시 이메일을 반환한다")
    void purpose_일치_PASSWORD_RESET_정상_파싱() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);

        // when
        String parsedEmail = provider.parseEmailVerificationToken(token, EmailVerificationPurpose.PASSWORD_RESET);

        // then
        assertThat(parsedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("CHANGE_EMAIL 로 발급한 토큰은 CHANGE_EMAIL 로 파싱 시 이메일을 반환한다")
    void purposeMatchesChangeEmail() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.CHANGE_EMAIL);

        // when
        String parsedEmail = provider.parseEmailVerificationToken(token, EmailVerificationPurpose.CHANGE_EMAIL);

        // then
        assertThat(parsedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("REGISTER 토큰을 PASSWORD_RESET 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다")
    void cross_purpose_REGISTER_to_RESET_거부() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.REGISTER);

        // when / then
        assertThatThrownBy(() ->
            provider.parseEmailVerificationToken(token, EmailVerificationPurpose.PASSWORD_RESET))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("PASSWORD_RESET 토큰을 REGISTER 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다")
    void cross_purpose_RESET_to_REGISTER_거부() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);

        // when / then
        assertThatThrownBy(() ->
            provider.parseEmailVerificationToken(token, EmailVerificationPurpose.REGISTER))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("REGISTER 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다")
    void registerTokenRejectedForChangeEmail() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.REGISTER);

        // when / then
        assertThatThrownBy(() ->
            provider.parseEmailVerificationToken(token, EmailVerificationPurpose.CHANGE_EMAIL))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("PASSWORD_RESET 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다")
    void passwordResetTokenRejectedForChangeEmail() {
        // given
        String token = provider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);

        // when / then
        assertThatThrownBy(() ->
            provider.parseEmailVerificationToken(token, EmailVerificationPurpose.CHANGE_EMAIL))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("RefreshToken 발급 시 jti와 만료시각을 포함하고 파싱 결과로 반환한다")
    void refresh_token_claims_파싱() {
        // given
        Long memberId = 10L;
        String token = provider.createRefreshToken(memberId);

        // when
        RefreshTokenClaims claims = provider.parseRefreshToken(token);

        // then
        assertThat(claims.memberId()).isEqualTo(memberId);
        assertThat(claims.jti()).isNotNull();
        assertThat(claims.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("AccessToken과 RefreshToken은 SSO client context claim을 포함하고 다시 파싱할 수 있다")
    void sso_client_context_claims_파싱() {
        // given
        Long memberId = 10L;
        ClientContextClaims clientContext = ClientContextClaims.of(
            "backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD
        );

        // when
        String accessToken = provider.createAccessToken(
            memberId,
            List.of("USER"),
            ClientType.WEB,
            clientContext,
            3600L
        );
        String refreshToken = provider.createRefreshToken(memberId, clientContext);

        // then
        assertThat(provider.getClientContextClaimsFromAccessToken(accessToken)).isEqualTo(clientContext);
        assertThat(provider.parseRefreshToken(refreshToken).clientContext()).isEqualTo(clientContext);
    }

    @Test
    @DisplayName("SSO login token은 전용 secret으로 서명하고 파싱한다")
    void sso_login_token_전용_secret_정상_파싱() {
        // given
        Long memberId = 10L;
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String token = provider.createSsoLoginToken(memberId, "email", expiresAt);

        // when
        SsoLoginTokenClaims claims = provider.parseSsoLoginToken(token);

        // then
        assertThat(claims.memberId()).isEqualTo(memberId);
        assertThat(claims.authenticationMethod()).isEqualTo("email");
        assertThat(claims.expiresAt()).isEqualTo(expiresAt.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("SSO login token secret이 AccessToken secret과 같으면 생성자에서 거부한다")
    void sso_login_token_secret_access_token_secret_중복_거부() {
        // when & then
        assertThatThrownBy(() -> new JwtTokenProvider(
            ACCESS_TOKEN_SECRET,
            REFRESH_TOKEN_SECRET,
            OAUTH_VERIFICATION_TOKEN_SECRET,
            EMAIL_VERIFICATION_TOKEN_SECRET,
            ACCESS_TOKEN_SECRET,
            3600L,
            3600L,
            600L
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sso-login-token-secret")
            .hasMessageContaining("access-token-secret")
            .hasMessageNotContaining(ACCESS_TOKEN_SECRET);
    }

    @Test
    @DisplayName("typ 이 SSO_LOGIN 인 토큰은 AccessToken secret으로 서명되어도 access token으로 거부한다")
    void typ_SSO_LOGIN_access_token_거부() {
        // given
        String token = createSsoLoginTypedTokenSignedWithAccessSecret();

        // when & then
        assertThatThrownBy(() -> provider.validateAccessToken(token))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_JWT);
        assertThatThrownBy(() -> provider.parseAccessToken(token))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_JWT);
    }

    @Test
    @DisplayName("OAuth verification secret으로 서명된 SSO login token은 거부한다")
    void sso_login_token_oauth_verification_secret_분리() {
        // given
        String token = createSsoLoginTypedTokenSignedWithSecret(OAUTH_VERIFICATION_TOKEN_SECRET);

        // when & then
        assertThatThrownBy(() -> provider.parseSsoLoginToken(token))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
    }

    private String createSsoLoginTypedTokenSignedWithAccessSecret() {
        return createSsoLoginTypedTokenSignedWithSecret(ACCESS_TOKEN_SECRET);
    }

    private String createSsoLoginTypedTokenSignedWithSecret(String secret) {
        Date now = new Date();
        Date expiresAt = Date.from(Instant.now().plusSeconds(3600));

        return Jwts.builder()
            .subject("10")
            .claim("typ", "SSO_LOGIN")
            .claim("auth", List.of("USER"))
            .issuedAt(now)
            .expiration(expiresAt)
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
}
