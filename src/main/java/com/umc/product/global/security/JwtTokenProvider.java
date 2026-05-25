package com.umc.product.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.umc.product.authentication.application.service.SsoLoginTokenClaims;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth"; // 권한 정보를 저장할 키
    private static final String CLIENT_TYPE_KEY = "clientType"; // 클라이언트 플랫폼(ANDROID/IOS/WEB) 정보를 저장할 키
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String CLIENT_SERVICE_KEY = "clientService";
    private static final String CLIENT_ENVIRONMENT_KEY = "clientEnvironment";
    private static final String SSO_LOGIN_TYPE = "SSO_LOGIN";
    private static final String TOKEN_TYPE_KEY = "typ";
    private static final String AUTHENTICATION_METHOD_KEY = "authenticationMethod";
    private static final String REQUIRED_TERMS_AGREED_KEY = "requiredTermsAgreed";
    private static final String AGREED_REQUIRED_TERM_IDS_KEY = "agreedRequiredTermIds";
    private final SecretKey accessTokenSecret;
    private final SecretKey refreshTokenSecret;
    private final SecretKey oAuthVerificationTokenSecret;
    private final SecretKey emailVerificationTokenSecret;
    private final SecretKey ssoLoginTokenSecret;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final long verificationTokenValidityInMilliseconds;

    public JwtTokenProvider(
        @Value("${jwt.access-token-secret}") String accessTokenSecret,
        @Value("${jwt.refresh-token-secret}") String refreshTokenSecret,
        @Value("${jwt.oauth-verification-token-secret}") String oAuthVerificationTokenSecret,
        @Value("${jwt.email-verification-token-secret}") String emailVerificationTokenSecret,
        @Value("${jwt.sso-login-token-secret}") String ssoLoginTokenSecret,
        @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
        @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds,
        @Value("${jwt.verification-token-validity-in-seconds}") long verificationTokenValidityInSeconds
    ) {

        validateSsoLoginTokenSecret(
            accessTokenSecret,
            refreshTokenSecret,
            oAuthVerificationTokenSecret,
            emailVerificationTokenSecret,
            ssoLoginTokenSecret
        );

        // SecretKey 객체로 안전하게 변환
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.oAuthVerificationTokenSecret = Keys.hmacShaKeyFor(oAuthVerificationTokenSecret
            .getBytes(StandardCharsets.UTF_8));
        this.emailVerificationTokenSecret = Keys.hmacShaKeyFor(emailVerificationTokenSecret
            .getBytes(StandardCharsets.UTF_8));
        this.ssoLoginTokenSecret = Keys.hmacShaKeyFor(ssoLoginTokenSecret.getBytes(StandardCharsets.UTF_8));

        this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
        this.verificationTokenValidityInMilliseconds = verificationTokenValidityInSeconds * 1000;
    }

    private void validateSsoLoginTokenSecret(
        String accessTokenSecret,
        String refreshTokenSecret,
        String oAuthVerificationTokenSecret,
        String emailVerificationTokenSecret,
        String ssoLoginTokenSecret
    ) {
        if (ssoLoginTokenSecret.equals(accessTokenSecret)) {
            throw new IllegalArgumentException(
                "jwt.sso-login-token-secret must be different from jwt.access-token-secret"
            );
        }
        if (ssoLoginTokenSecret.equals(refreshTokenSecret)) {
            throw new IllegalArgumentException(
                "jwt.sso-login-token-secret must be different from jwt.refresh-token-secret"
            );
        }
        if (ssoLoginTokenSecret.equals(oAuthVerificationTokenSecret)) {
            throw new IllegalArgumentException(
                "jwt.sso-login-token-secret must be different from jwt.oauth-verification-token-secret"
            );
        }
        if (ssoLoginTokenSecret.equals(emailVerificationTokenSecret)) {
            throw new IllegalArgumentException(
                "jwt.sso-login-token-secret must be different from jwt.email-verification-token-secret"
            );
        }
    }

    /**
     * oAuthVerificationToken 발급 (email, provider, providerId)
     */
    public String createOAuthVerificationToken(String email, OAuthProvider provider, String providerId) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + verificationTokenValidityInMilliseconds); // 10분 유효

        return Jwts.builder()
            .subject("OAUTH_VERIFICATION")
            .claim("email", email)
            .claim("provider", provider)
            .claim("providerId", providerId)
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(oAuthVerificationTokenSecret)
            .compact();
    }

    /**
     * emailVerificationToken 발급
     * <p>
     * purpose claim 으로 회원가입(REGISTER), 비밀번호 초기화(PASSWORD_RESET), 이메일 변경(CHANGE_EMAIL) 흐름을 구분한다.
     * 한 흐름에서 발급된 토큰이 다른 흐름에 재사용되지 않도록, 파싱 시 expectedPurpose 와 비교한다.
     */
    public String createEmailVerificationToken(String email, EmailVerificationPurpose purpose) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + verificationTokenValidityInMilliseconds); // 10분 유효

        return Jwts.builder()
            .subject("EMAIL_VERIFICATION")
            .claim("email", email)
            .claim("purpose", purpose.name())
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(emailVerificationTokenSecret)
            .compact();
    }

    /**
     * SSO Auth App 브라우저 로그인 상태를 표현하는 stateless JWT.
     * <p>
     * 일반 API 인증용 AccessToken/RefreshToken 과 분리하기 위해 {@code typ=SSO_LOGIN} claim 을 포함하고,
     * 일반 API 인증용 AccessToken/RefreshToken 및 OAuth 검증 토큰과 분리된 전용 secret 으로 서명한다.
     */
    public String createSsoLoginToken(Long memberId, String authenticationMethod, Instant expiresAt) {
        Date now = new Date();

        return Jwts.builder()
            .subject(String.valueOf(memberId))
            .claim(TOKEN_TYPE_KEY, SSO_LOGIN_TYPE)
            .claim(AUTHENTICATION_METHOD_KEY, authenticationMethod)
            .issuedAt(now)
            .expiration(Date.from(expiresAt))
            .signWith(ssoLoginTokenSecret)
            .compact();
    }

    /**
     * AccessToken 생성 메소드
     */
    public String createAccessToken(Long memberId, List<String> roles) {
        return createAccessToken(memberId, roles, (ClientType) null);
    }

    /**
     * AccessToken 생성 메소드 (clientType 포함)
     * <p>
     * clientType 은 트래픽 분포 분석용 optional claim. null 인 경우 claim 자체를 추가하지 않으며,
     * 다운스트림(MDC, 통계)에서는 UNKNOWN 으로 집계된다.
     */
    public String createAccessToken(Long memberId, List<String> roles, ClientType clientType) {
        return createAccessTokenInternal(
            memberId,
            roles,
            clientType,
            null,
            null,
            null,
            accessTokenValidityInMilliseconds
        );
    }

    public String createAccessToken(
        Long memberId,
        List<String> roles,
        ClientType clientType,
        boolean requiredTermsAgreed
    ) {
        return createAccessToken(memberId, roles, clientType, requiredTermsAgreed, List.of());
    }

    public String createAccessToken(
        Long memberId,
        List<String> roles,
        ClientType clientType,
        boolean requiredTermsAgreed,
        List<Long> agreedRequiredTermIds
    ) {
        return createAccessTokenInternal(
            memberId,
            roles,
            clientType,
            null,
            requiredTermsAgreed,
            agreedRequiredTermIds,
            accessTokenValidityInMilliseconds
        );
    }

    public String createAccessToken(
        Long memberId,
        List<String> roles,
        ClientType clientType,
        ClientContextClaims clientContext,
        Duration expiresIn
    ) {
        return createAccessToken(memberId, roles, clientType, clientContext, expiresIn.toSeconds());
    }

    public String createAccessToken(
        Long memberId,
        List<String> roles,
        ClientType clientType,
        ClientContextClaims clientContext,
        Long expiresInSeconds
    ) {
        return createAccessTokenInternal(
            memberId,
            roles,
            clientType,
            clientContext,
            null,
            null,
            expiresInSeconds * 1000
        );
    }

    private String createAccessTokenInternal(
        Long memberId,
        List<String> roles,
        ClientType clientType,
        ClientContextClaims clientContext,
        Boolean requiredTermsAgreed,
        List<Long> agreedRequiredTermIds,
        long validityInMilliseconds
    ) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + validityInMilliseconds);

        var builder = Jwts.builder()
            .subject(String.valueOf(memberId))
            .claim(AUTHORITIES_KEY, roles)
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(accessTokenSecret);

        if (clientType != null) {
            builder.claim(CLIENT_TYPE_KEY, clientType.name());
        }
        addClientContextClaims(builder, clientContext);
        if (requiredTermsAgreed != null) {
            builder.claim(REQUIRED_TERMS_AGREED_KEY, requiredTermsAgreed);
        }
        if (agreedRequiredTermIds != null) {
            builder.claim(AGREED_REQUIRED_TERM_IDS_KEY, agreedRequiredTermIds);
        }

        return builder.compact();
    }

    public String createAccessToken(Long memberId, List<String> roles, Long expiresInSeconds) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + expiresInSeconds * 1000);

        return Jwts.builder()
            .subject(String.valueOf(memberId)) // 사용자 식별자 (ID)
            .claim(AUTHORITIES_KEY, roles)     // 권한 정보 저장
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(accessTokenSecret)
            .compact();
    }

    // 2. Refresh Token 생성
    public String createRefreshToken(Long memberId) {
        // Refresh Token에는 권한 정보 등을 담지 않는 것이 일반적입니다.
        // 필요하다면 roles를 null 대신 넣어도 됩니다.
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
            .subject(String.valueOf(memberId)) // 사용자 식별자 (ID)
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(refreshTokenSecret)
            .compact();
    }

    public String createRefreshToken(Long memberId, ClientContextClaims clientContext) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        var builder = Jwts.builder()
            .subject(String.valueOf(memberId))
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(validityDate)
            .signWith(refreshTokenSecret);

        addClientContextClaims(builder, clientContext);

        return builder.compact();
    }

    public List<String> getRolesFromAccessToken(String token) {
        Claims claims = parseAccessTokenClaims(token);
        Object roles = claims.get(AUTHORITIES_KEY);
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        return Collections.emptyList();
    }

    /**
     * AccessToken 에서 clientType claim 을 추출한다.
     * <p>
     * 도입 이전에 발급된 토큰 / clientType 미전달 로그인 경로로 발급된 토큰에는 claim 이 존재하지 않으므로,
     * 그 경우엔 {@code null} 을 반환한다. 호출자는 null-safe 하게 다루어야 하며
     * 통계에서는 "UNKNOWN" 으로 집계한다. 절대 예외를 던지지 않는다.
     */
    public ClientType getClientTypeFromAccessToken(String token) {
        Claims claims = parseAccessTokenClaims(token);
        String clientTypeStr = claims.get(CLIENT_TYPE_KEY, String.class);
        if (clientTypeStr == null) {
            return null;
        }
        try {
            return ClientType.valueOf(clientTypeStr);
        } catch (IllegalArgumentException e) {
            // 알 수 없는 enum 값이 들어와도 통계 집계가 깨지지 않도록 null 처리.
            log.warn("AccessToken 의 clientType claim 값을 해석할 수 없습니다: {}", clientTypeStr);
            return null;
        }
    }

    public ClientContextClaims getClientContextClaimsFromAccessToken(String token) {
        return getClientContextClaims(parseAccessTokenClaims(token));
    }

    /**
     * AccessToken 에서 필수 약관 동의 완료 여부를 추출한다.
     * <p>
     * claim 도입 이전에 발급된 토큰에는 값이 없으므로 하위 호환을 위해 동의 완료로 간주한다.
     * 실제 최신 상태는 로그인/토큰 재발급 시점에 다시 계산해 새 AccessToken claim 으로 반영한다.
     */
    public boolean hasRequiredTermsAgreed(String token) {
        Claims claims = parseAccessTokenClaims(token);
        Boolean requiredTermsAgreed = claims.get(REQUIRED_TERMS_AGREED_KEY, Boolean.class);
        return requiredTermsAgreed == null || requiredTermsAgreed;
    }

    public List<Long> getAgreedRequiredTermIdsFromAccessToken(String token) {
        Claims claims = parseAccessTokenClaims(token);
        Object agreedRequiredTermIds = claims.get(AGREED_REQUIRED_TERM_IDS_KEY);
        if (agreedRequiredTermIds instanceof List<?> ids) {
            return ids.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
        }
        return Collections.emptyList();
    }

    /**
     * JWT 서명 검증(HMAC-SHA)을 요청당 1회로 한정하여 memberId, roles, clientType을 반환한다. clientType claim이 없거나 알 수 없는 값이면 null을
     * 반환한다.
     */
    @SuppressWarnings("unchecked")
    public ParsedAccessToken parseAndValidateAccessToken(String token) {
        try {
            Claims claims = parseAccessTokenClaims(token);
            Long memberId = Long.parseLong(claims.getSubject());

            Object rolesObj = claims.get(AUTHORITIES_KEY);
            List<String> roles = (rolesObj instanceof List<?>) ? (List<String>) rolesObj : Collections.emptyList();

            String clientTypeStr = claims.get(CLIENT_TYPE_KEY, String.class);
            ClientType clientType = null;
            if (clientTypeStr != null) {
                try {
                    clientType = ClientType.valueOf(clientTypeStr);
                } catch (IllegalArgumentException e) {
                    log.warn("AccessToken 의 clientType claim 값을 해석할 수 없습니다: {}", clientTypeStr);
                }
            }

            return new ParsedAccessToken(memberId, roles, clientType);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.UNSUPPORTED_JWT);
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessTokenClaims(token);
            return true;
        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.debug("잘못된 JWT 서명입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.debug("지원되지 않는 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.UNSUPPORTED_JWT);
        } catch (IllegalArgumentException e) {
            log.debug("JWT token 형식이 올바르지 않습니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }
    }

    // 4. 토큰 검증
    private boolean validateToken(String token, SecretKey secretKey) {
        try {
            parseClaims(token, secretKey);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.debug("잘못된 JWT 서명입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.WRONG_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.debug("지원되지 않는 JWT 토큰입니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.UNSUPPORTED_JWT);
        } catch (IllegalArgumentException e) {
            log.debug("JWT token 형식이 올바르지 않습니다.");
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }
//        return false;
    }

    // 토큰 파싱 내부 함수
    private Claims parseClaims(String token, SecretKey secretKey) {

        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * AccessToken에서 memberId를 추출하는데 사용됩니다.
     */
    public Long parseAccessToken(String token) {
        return Long.parseLong(parseAccessTokenClaims(token).getSubject());
    }

    private Claims parseAccessTokenClaims(String token) {
        Claims claims = parseClaims(token, accessTokenSecret);
        rejectSsoLoginTokenType(claims);
        return claims;
    }

    private void rejectSsoLoginTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_KEY, String.class);
        if (SSO_LOGIN_TYPE.equals(tokenType)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }
    }

    /**
     * RefreshToken의 정보를 파싱해서 allow-list 식별에 필요한 claims 를 반환합니다.
     */
    public RefreshTokenClaims parseRefreshToken(String token) {
        validateToken(token, refreshTokenSecret);

        Claims claims = parseClaims(token, refreshTokenSecret);
        String jti = claims.getId();
        if (jti == null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            return new RefreshTokenClaims(
                Long.parseLong(claims.getSubject()),
                UUID.fromString(jti),
                toInstant(claims.getExpiration()),
                getClientContextClaims(claims)
            );
        } catch (IllegalArgumentException e) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private Instant toInstant(Date date) {
        if (date == null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }
        return date.toInstant();
    }

    private void addClientContextClaims(io.jsonwebtoken.JwtBuilder builder, ClientContextClaims clientContext) {
        ClientContextClaims claims = clientContext == null ? ClientContextClaims.empty() : clientContext;
        if (claims.clientId() != null && !claims.clientId().isBlank()) {
            builder.claim(CLIENT_ID_KEY, claims.clientId());
            builder.audience().add(claims.clientId()).and();
        }
        builder.claim(CLIENT_SERVICE_KEY, claims.serviceType().name());
        builder.claim(CLIENT_ENVIRONMENT_KEY, claims.environment().name());
    }

    private ClientContextClaims getClientContextClaims(Claims claims) {
        String clientId = claims.get(CLIENT_ID_KEY, String.class);
        String clientService = claims.get(CLIENT_SERVICE_KEY, String.class);
        String clientEnvironment = claims.get(CLIENT_ENVIRONMENT_KEY, String.class);

        if (clientId == null && clientService == null && clientEnvironment == null) {
            return ClientContextClaims.empty();
        }

        return ClientContextClaims.of(
            clientId,
            parseClientServiceType(clientService),
            parseClientEnvironment(clientEnvironment)
        );
    }

    private ClientServiceType parseClientServiceType(String value) {
        if (value == null) {
            return ClientServiceType.UNKNOWN;
        }
        try {
            return ClientServiceType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 의 clientService claim 값을 해석할 수 없습니다: {}", value);
            return ClientServiceType.UNKNOWN;
        }
    }

    private ClientEnvironment parseClientEnvironment(String value) {
        if (value == null) {
            return ClientEnvironment.UNKNOWN;
        }
        try {
            return ClientEnvironment.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 의 clientEnvironment claim 값을 해석할 수 없습니다: {}", value);
            return ClientEnvironment.UNKNOWN;
        }
    }

    /**
     * oAuthVerificationToken 파싱 및 검증
     */
    public OAuthVerificationClaims parseOAuthVerificationToken(String token) {
        validateToken(token, oAuthVerificationTokenSecret);

        Claims claims = parseClaims(token, oAuthVerificationTokenSecret);

        String email = claims.get("email", String.class);
        String providerStr = claims.get("provider", String.class);
        String providerId = claims.get("providerId", String.class);

        OAuthProvider provider = OAuthProvider.valueOf(providerStr);

        return new OAuthVerificationClaims(email, provider, providerId);
    }

    /**
     * emailVerificationToken 파싱 및 검증
     * <p>
     * 토큰의 purpose claim 이 expectedPurpose 와 일치하지 않으면 INVALID_EMAIL_VERIFICATION 예외를 던진다.
     * 예) 회원가입(REGISTER) 흐름에서 발급된 토큰을 비밀번호 초기화나 이메일 변경에 사용하려는 cross-purpose 공격 방어.
     */
    public String parseEmailVerificationToken(String token, EmailVerificationPurpose expectedPurpose) {
        validateToken(token, emailVerificationTokenSecret);

        Claims claims = parseClaims(token, emailVerificationTokenSecret);

        String purposeClaim = claims.get("purpose", String.class);
        if (purposeClaim == null || !purposeClaim.equals(expectedPurpose.name())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        return claims.get("email", String.class);
    }

    public SsoLoginTokenClaims parseSsoLoginToken(String token) {
        try {
            Claims claims = parseClaims(token, ssoLoginTokenSecret);
            String tokenType = claims.get(TOKEN_TYPE_KEY, String.class);
            String authenticationMethod = claims.get(AUTHENTICATION_METHOD_KEY, String.class);

            if (!SSO_LOGIN_TYPE.equals(tokenType) || authenticationMethod == null || authenticationMethod.isBlank()) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
            }

            return SsoLoginTokenClaims.of(
                Long.parseLong(claims.getSubject()),
                toInstant(claims.getIssuedAt(), AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN),
                toInstant(claims.getExpiration(), AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN),
                authenticationMethod
            );
        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
        }
    }

    private Instant toInstant(Date date, AuthenticationErrorCode errorCode) {
        if (date == null) {
            throw new AuthenticationDomainException(errorCode);
        }
        return date.toInstant();
    }
}
