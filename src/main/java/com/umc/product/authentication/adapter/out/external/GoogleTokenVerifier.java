package com.umc.product.authentication.adapter.out.external;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.logging.ExternalApiCallLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google ID 토큰 검증 Adapter
 * <p>
 * Google OIDC ID Token은 JWKS 공개키로 로컬 검증하고, 기존 access token은 tokeninfo endpoint로 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final RestClient restClient;
    private final OidcPublicKeyResolver publicKeyResolver;
    private final GoogleOAuthProperties googleProperties;

    /**
     * Google OAuth 토큰을 검증하고 OAuthAttributes로 변환합니다.
     *
     * @param token Google에서 발급받은 ID Token 또는 기존 Access Token
     * @return OAuthAttributes
     * @throws AuthenticationDomainException 토큰 검증 실패 시
     */
    public OAuthAttributes verify(String token) {
        if (isJwt(token)) {
            return verifyIdToken(token);
        }
        return verifyAccessToken(token);
    }

    public OAuthAttributes verifyIdToken(String idToken) {
        log.debug("Google ID Token 검증 시작");

        try {
            String kid = publicKeyResolver.extractKid(idToken);
            Claims claims = Jwts.parser()
                .verifyWith(publicKeyResolver.getPublicKey(googleJwksSpec(), kid))
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

            if (!GOOGLE_ISSUERS.contains(claims.getIssuer())) {
                log.warn("Google ID 토큰 issuer 불일치: actual={}", claims.getIssuer());
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
            }

            Set<String> audience = claims.getAudience();
            if (audience == null || audience.stream().noneMatch(googleProperties.clientIdList()::contains)) {
                log.warn("Google ID 토큰 audience 불일치: expected={}, actual={}",
                    googleProperties.clientIdList(), audience);
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
            }

            String sub = claims.getSubject();
            String email = claims.get("email", String.class);
            log.debug("Google ID Token을 검증했습니다: hasEmail={}", hasEmail(email));

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", sub);
            attributes.put("email", email);
            attributes.put("name", claims.get("name", String.class));
            attributes.put("picture", claims.get("picture", String.class));

            return OAuthAttributes.of("google", attributes);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID Token 검증 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    private OAuthAttributes verifyAccessToken(String accessToken) {
        log.debug("Google Access Token 검증 시작");

        try {
            GoogleAccessTokenInfoResponse response = ExternalApiCallLogger.measure(
                "GOOGLE",
                "VERIFY_ACCESS_TOKEN",
                () ->
                restClient.get()
                    .uri(GOOGLE_TOKEN_INFO_URL + "?access_token=" + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Google tokeninfo 호출 실패: status={}", res.getStatusCode());
                        throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN);
                    })
                    .body(GoogleAccessTokenInfoResponse.class)
            );

            if (response == null) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }

            if (!googleProperties.clientIdList().contains(response.aud())) {
                log.warn("Google ID 토큰 audience 불일치: expected={}, actual={}",
                    googleProperties.clientIdList(), response.aud());
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
            }

            log.debug("Google Access Token을 검증했습니다: hasEmail={}", hasEmail(response.email()));

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", response.sub());
            attributes.put("email", response.email());

            return OAuthAttributes.of("google", attributes);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google Access Token 검증 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Google OAuth 토큰을 revoke합니다.
     *
     * @param token revoke할 토큰 (access token 또는 refresh token)
     * @see <a href="https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke">Google Token
     * Revoke</a>
     */
    public void revokeToken(String token) {
        log.info("Google token revoke 시작");

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("token", token);

            ExternalApiCallLogger.measure("GOOGLE", "REVOKE_TOKEN", () ->
                restClient.post()
                    .uri(GOOGLE_REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Google token revoke 실패: status={}", res.getStatusCode());
                        throw new AuthenticationDomainException(
                            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
                    })
                    .toBodilessEntity()
            );

            log.info("Google token revoke를 완료했습니다");

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google token revoke 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    private boolean isJwt(String token) {
        return StringUtils.hasText(token) && token.split("\\.").length == 3;
    }

    private OidcJwksSpec googleJwksSpec() {
        return new OidcJwksSpec(
            CacheNamespace.GOOGLE_JWKS,
            GOOGLE_JWKS_URL,
            googleProperties.jwksCache().ttl(),
            googleProperties.jwksCache().maxSize()
        );
    }

    private boolean hasEmail(String email) {
        return email != null && !email.isBlank();
    }

    private record GoogleAccessTokenInfoResponse(
        String sub,
        String email,
        String aud
    ) {
    }
}
