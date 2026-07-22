package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@DisplayName("JwtTokenProvider 잔여 보안 계약")
class JwtTokenProviderResidualTest {

    private static final String ACCESS = "residual-access-secret-must-be-long-enough-for-hmac-sha256";
    private static final String REFRESH = "residual-refresh-secret-must-be-long-enough-for-hmac-sha256";
    private static final String OAUTH = "residual-oauth-secret-must-be-long-enough-for-hmac-sha256";
    private static final String EMAIL = "residual-email-secret-must-be-long-enough-for-hmac-sha256";
    private static final String SSO = "residual-sso-secret-must-be-long-enough-for-hmac-sha256";
    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = provider(ACCESS, REFRESH, OAUTH, EMAIL, SSO, 3600L);
    }

    @Test
    @DisplayName("SSO 전용 secret은 refresh·OAuth·email verification secret과도 같을 수 없다")
    void SSO_secret_격리를_검증한다() {
        assertThatThrownBy(() -> provider(ACCESS, REFRESH, OAUTH, EMAIL, REFRESH, 3600L))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("refresh-token-secret");
        assertThatThrownBy(() -> provider(ACCESS, REFRESH, OAUTH, EMAIL, OAUTH, 3600L))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("oauth-verification-token-secret");
        assertThatThrownBy(() -> provider(ACCESS, REFRESH, OAUTH, EMAIL, EMAIL, 3600L))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email-verification-token-secret");
    }

    @Test
    @DisplayName("OAuth verification token은 email·provider·providerId를 손실 없이 왕복한다")
    void OAuth_verification_token을_왕복한다() {
        String token = sut.createOAuthVerificationToken("user@example.com", OAuthProvider.GOOGLE, "provider-id");

        assertThat(sut.parseOAuthVerificationToken(token))
            .returns("user@example.com", OAuthVerificationClaims::email)
            .returns(OAuthProvider.GOOGLE, OAuthVerificationClaims::provider)
            .returns("provider-id", OAuthVerificationClaims::providerId);
    }

    @Test
    @DisplayName("Access token 생성 overload는 역할·회원·client type과 만료 설정을 보존한다")
    void Access_token_overload를_왕복한다() {
        String basic = sut.createAccessToken(1L, List.of("USER"));
        String withClient = sut.createAccessToken(2L, List.of("ADMIN"), ClientType.WEB);
        String withSeconds = sut.createAccessToken(3L, List.of("USER"), 60L);
        String withDuration = sut.createAccessToken(
            4L, List.of("USER"), ClientType.IOS, ClientContextClaims.empty(), Duration.ofMinutes(1));

        assertThat(sut.parseAccessToken(basic)).isEqualTo(1L);
        assertThat(sut.getRolesFromAccessToken(basic)).containsExactly("USER");
        assertThat(sut.getClientTypeFromAccessToken(basic)).isNull();
        assertThat(sut.getClientTypeFromAccessToken(withClient)).isEqualTo(ClientType.WEB);
        assertThat(sut.parseAndValidateAccessToken(withSeconds).memberId()).isEqualTo(3L);
        assertThat(sut.parseAndValidateAccessToken(withDuration).clientType()).isEqualTo(ClientType.IOS);
        assertThat(sut.validateAccessToken(basic)).isTrue();
    }

    @Test
    @DisplayName("Access token client context는 blank client ID를 audience 없이 UNKNOWN 기본값과 함께 보존한다")
    void blank_client_context를_정규화한다() {
        String token = sut.createAccessToken(
            1L,
            List.of(),
            null,
            ClientContextClaims.of(" ", null, null),
            60L
        );

        assertThat(sut.getClientContextClaimsFromAccessToken(token))
            .isEqualTo(ClientContextClaims.empty());
    }

    @Test
    @DisplayName("알 수 없는 client enum과 roles 타입은 null·UNKNOWN·빈 목록으로 fail-soft 처리한다")
    void 알_수_없는_client_claim을_fail_soft한다() {
        String token = signed(ACCESS, builder -> builder
            .subject("1")
            .claim("auth", "USER")
            .claim("clientType", "DESKTOP")
            .claim("clientService", "UNKNOWN_SERVICE")
            .claim("clientEnvironment", "UNKNOWN_ENV")
            .expiration(Date.from(Instant.now().plusSeconds(60))));

        assertThat(sut.getRolesFromAccessToken(token)).isEmpty();
        assertThat(sut.getClientTypeFromAccessToken(token)).isNull();
        assertThat(sut.parseAndValidateAccessToken(token).clientType()).isNull();
        assertThat(sut.getClientContextClaimsFromAccessToken(token))
            .returns(ClientServiceType.UNKNOWN, ClientContextClaims::serviceType)
            .returns(ClientEnvironment.UNKNOWN, ClientContextClaims::environment);
    }

    @Test
    @DisplayName("client ID만 있는 legacy token은 service와 environment를 UNKNOWN으로 복원한다")
    void legacy_client_context의_누락_enum을_복원한다() {
        String token = signed(ACCESS, builder -> builder.subject("1")
            .claim("clientId", "legacy-client")
            .expiration(Date.from(Instant.now().plusSeconds(60))));

        assertThat(sut.getClientContextClaimsFromAccessToken(token))
            .returns("legacy-client", ClientContextClaims::clientId)
            .returns(ClientServiceType.UNKNOWN, ClientContextClaims::serviceType)
            .returns(ClientEnvironment.UNKNOWN, ClientContextClaims::environment);
    }

    @Test
    @DisplayName("Refresh token context는 null 입력을 빈 context로 정규화한다")
    void Refresh_token_null_context를_정규화한다() {
        RefreshTokenClaims claims = sut.parseRefreshToken(sut.createRefreshToken(1L, null));

        assertThat(claims.clientContext()).isEqualTo(ClientContextClaims.empty());
    }

    @Test
    @DisplayName("Refresh token은 jti 누락·잘못된 UUID·만료 누락을 모두 거부한다")
    void 잘못된_Refresh_token_claim을_거부한다() {
        String noJti = signed(REFRESH, builder -> builder.subject("1")
            .expiration(Date.from(Instant.now().plusSeconds(60))));
        String invalidJti = signed(REFRESH, builder -> builder.subject("1").id("not-uuid")
            .expiration(Date.from(Instant.now().plusSeconds(60))));
        String noExpiration = signed(REFRESH, builder -> builder.subject("1").id(UUID.randomUUID().toString()));

        assertRefreshInvalid(noJti);
        assertRefreshInvalid(invalidJti);
        assertRefreshInvalid(noExpiration);
    }

    @Test
    @DisplayName("OAuth provider claim이 알 수 없는 값이면 검증 token을 거부한다")
    void 잘못된_OAuth_provider를_거부한다() {
        String token = signed(OAUTH, builder -> builder.subject("OAUTH_VERIFICATION")
            .claim("email", "user@example.com")
            .claim("provider", "UNKNOWN")
            .claim("providerId", "id")
            .expiration(Date.from(Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> sut.parseOAuthVerificationToken(token))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Access token의 잘못된 서명·만료·형식은 안정적인 인증 error code로 변환한다")
    void Access_token_오류를_정규화한다() {
        String wrongSignature = provider(
            "another-access-secret-must-be-long-enough-for-hmac-sha256", REFRESH, OAUTH, EMAIL, SSO, 3600L)
            .createAccessToken(1L, List.of());
        String expired = provider(ACCESS, REFRESH, OAUTH, EMAIL, SSO, -1L)
            .createAccessToken(1L, List.of());
        String unsupported = Jwts.builder().subject("1").compact();

        assertAccessError(() -> sut.parseAndValidateAccessToken(wrongSignature), AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        assertAccessError(() -> sut.parseAndValidateAccessToken(expired), AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        assertAccessError(
            () -> sut.parseAndValidateAccessToken("not-a-jwt"), AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        assertAccessError(() -> sut.parseAndValidateAccessToken(unsupported), AuthenticationErrorCode.UNSUPPORTED_JWT);
        assertAccessError(() -> sut.validateAccessToken(wrongSignature), AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        assertAccessError(() -> sut.validateAccessToken(expired), AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        assertAccessError(() -> sut.validateAccessToken(unsupported), AuthenticationErrorCode.UNSUPPORTED_JWT);
        assertAccessError(() -> sut.validateAccessToken(""), AuthenticationErrorCode.INVALID_JWT);
    }

    @Test
    @DisplayName("verification token 공통 검증은 잘못된 서명·만료·미지원·빈 형식을 정규화한다")
    void verification_token_공통_검증_오류를_정규화한다() {
        String wrongSignature = signed(ACCESS, builder -> builder.subject("OAUTH_VERIFICATION")
            .expiration(Date.from(Instant.now().plusSeconds(60))));
        String expired = signed(OAUTH, builder -> builder.subject("OAUTH_VERIFICATION")
            .expiration(Date.from(Instant.now().minusSeconds(1))));
        String unsupported = Jwts.builder().subject("OAUTH_VERIFICATION").compact();

        assertAccessError(
            () -> sut.parseOAuthVerificationToken(wrongSignature), AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        assertAccessError(() -> sut.parseOAuthVerificationToken(expired), AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        assertAccessError(
            () -> sut.parseOAuthVerificationToken(unsupported), AuthenticationErrorCode.UNSUPPORTED_JWT);
        assertAccessError(() -> sut.parseOAuthVerificationToken(""), AuthenticationErrorCode.INVALID_JWT);
    }

    @Test
    @DisplayName("Access token subject가 숫자가 아니면 parseAndValidate는 INVALID_JWT로 변환한다")
    void 숫자가_아닌_subject를_거부한다() {
        String token = signed(ACCESS, builder -> builder.subject("member")
            .claim("auth", List.of())
            .expiration(Date.from(Instant.now().plusSeconds(60))));

        assertAccessError(() -> sut.parseAndValidateAccessToken(token), AuthenticationErrorCode.INVALID_JWT);
    }

    @Test
    @DisplayName("email verification token의 purpose 누락은 cross-purpose와 동일하게 거부한다")
    void email_purpose_누락을_거부한다() {
        String token = signed(EMAIL, builder -> builder.subject("EMAIL_VERIFICATION")
            .claim("email", "user@example.com")
            .expiration(Date.from(Instant.now().plusSeconds(60))));

        assertAccessError(
            () -> sut.parseEmailVerificationToken(token, EmailVerificationPurpose.REGISTER),
            AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("SSO login token은 type·인증 방식·회원 ID·시각 누락과 형식 오류를 모두 거부한다")
    void 잘못된_SSO_login_claim을_거부한다() {
        Date future = Date.from(Instant.now().plusSeconds(60));
        String noType = signed(SSO, builder -> builder.subject("1").claim("authenticationMethod", "email")
            .issuedAt(new Date()).expiration(future));
        String blankMethod = signed(SSO, builder -> builder.subject("1").claim("typ", "SSO_LOGIN")
            .claim("authenticationMethod", " ").issuedAt(new Date()).expiration(future));
        String invalidMember = signed(SSO, builder -> builder.subject("member").claim("typ", "SSO_LOGIN")
            .claim("authenticationMethod", "email").issuedAt(new Date()).expiration(future));
        String noIssuedAt = Jwts.builder()
            .subject("1")
            .claim("typ", "SSO_LOGIN")
            .claim("authenticationMethod", "email")
            .expiration(future)
            .signWith(key(SSO))
            .compact();

        assertSsoInvalid(noType);
        assertSsoInvalid(blankMethod);
        assertSsoInvalid(invalidMember);
        assertSsoInvalid(noIssuedAt);
        assertSsoInvalid("not-a-jwt");
    }

    private JwtTokenProvider provider(
        String access, String refresh, String oauth, String email, String sso, long accessValiditySeconds
    ) {
        return new JwtTokenProvider(access, refresh, oauth, email, sso, accessValiditySeconds, 3600L, 600L);
    }

    private String signed(String secret, java.util.function.Consumer<JwtBuilder> customizer) {
        JwtBuilder builder = Jwts.builder().issuedAt(new Date());
        customizer.accept(builder);
        return builder.signWith(key(secret)).compact();
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private void assertRefreshInvalid(String token) {
        assertAccessError(() -> sut.parseRefreshToken(token), AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
    }

    private void assertSsoInvalid(String token) {
        assertAccessError(() -> sut.parseSsoLoginToken(token), AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
    }

    private void assertAccessError(Runnable action, AuthenticationErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(errorCode);
    }
}
