package com.umc.product.authentication.adapter.out.external;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Kakao OAuth Token 검증 Adapter
 * <p>
 * Kakao OIDC ID Token은 JWKS 공개키로 로컬 검증하고, 기존 Access Token은 userinfo API로 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTokenVerifier {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";
    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final OidcPublicKeyResolver publicKeyResolver;

    /**
     * Kakao Authorization Code를 교환하고 OAuthAttributes로 변환합니다.
     * <p>
     * 동작:
     * <ol>
     *     <li>전달받은 redirect URI가 화이트리스트에 포함되는지 검증한다.</li>
     *     <li>Kakao token endpoint에 grant_type=authorization_code로 POST하여 토큰을 교환한다.</li>
     *     <li>응답에 id_token이 있으면 OIDC 서명 검증을 사용하고, 없으면 기존 access token userinfo 조회로 fallback한다.</li>
     * </ol>
     *
     * @param authorizationCode Kakao에서 발급받은 authorization code
     * @param redirectUri       클라이언트가 Kakao 인가 요청에 사용한 redirect URI (화이트리스트와 일치해야 함)
     * @return OAuthAttributes
     * @throws AuthenticationDomainException redirect URI 불일치, 토큰 교환 실패, 또는 사용자 정보 조회 실패 시
     */
    public OAuthAttributes verifyAuthorizationCode(String authorizationCode, String redirectUri) {
        log.debug("Kakao Authorization Code 교환 시작");

        if (!kakaoOAuthProperties.isAllowedRedirectUri(redirectUri)) {
            log.warn("허용되지 않은 Kakao redirect URI: {}", redirectUri);
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_REDIRECT_URI);
        }

        KakaoTokenResponse response = exchangeAuthorizationCode(authorizationCode, redirectUri);
        if (StringUtils.hasText(response.idToken())) {
            return verifyIdToken(response.idToken());
        }
        return verifyAccessToken(response.accessToken());
    }

    /**
     * Kakao token endpoint를 호출하여 authorization code를 access token으로 교환합니다.
     * <p>
     * client_secret은 Kakao 앱이 보안 모드일 때만 필요하며, 비어 있으면 form data에서 제외합니다.
     */
    private KakaoTokenResponse exchangeAuthorizationCode(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoOAuthProperties.clientId());
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authorizationCode);
        if (kakaoOAuthProperties.clientSecret() != null && !kakaoOAuthProperties.clientSecret().isBlank()) {
            formData.add("client_secret", kakaoOAuthProperties.clientSecret());
        }

        try {
            KakaoTokenResponse response = ExternalApiCallLogger.measure("KAKAO", "EXCHANGE_AUTHORIZATION_CODE", () ->
                restClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao 토큰 교환 실패: status={}", res.getStatusCode());
                        throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
                    })
                    .body(KakaoTokenResponse.class)
            );

            if (response == null || (!StringUtils.hasText(response.idToken())
                && !StringUtils.hasText(response.accessToken()))) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }

            log.info("Kakao 토큰을 교환했습니다");
            return response;

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao 토큰 교환 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Kakao Access Token을 검증하고 OAuthAttributes로 변환합니다.
     * <p>
     * Kakao API를 호출하여 사용자 정보를 조회함으로써 토큰의 유효성을 검증합니다.
     *
     * @param accessToken Kakao에서 발급받은 Access Token
     * @return OAuthAttributes
     * @throws AuthenticationDomainException 토큰 검증 실패 시
     */
    public OAuthAttributes verifyAccessToken(String accessToken) {
        log.debug("Kakao Access Token 검증 시작");

        try {
            KakaoUserResponse response = ExternalApiCallLogger.measure("KAKAO", "GET_USER_INFO", () ->
                restClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao 사용자 정보 조회 실패: status={}", res.getStatusCode());
                        throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
                    })
                    .body(KakaoUserResponse.class)
            );

            if (response == null || response.id() == null) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }

            log.debug("Kakao Access Token을 검증했습니다: hasEmail={}", hasEmail(response));

            Map<String, Object> attributes = buildAttributesMap(response);

            return OAuthAttributes.of("kakao", attributes);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao Access Token 검증 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    public OAuthAttributes verify(String token) {
        if (isJwt(token)) {
            return verifyIdToken(token);
        }
        return verifyAccessToken(token);
    }

    public OAuthAttributes verifyIdToken(String idToken) {
        log.debug("Kakao ID Token 검증 시작");

        try {
            String kid = publicKeyResolver.extractKid(idToken);
            Claims claims = Jwts.parser()
                .verifyWith(publicKeyResolver.getPublicKey(kakaoJwksSpec(), kid))
                .requireIssuer(KAKAO_ISSUER)
                .requireAudience(kakaoOAuthProperties.clientId())
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

            log.debug("Kakao ID Token을 검증했습니다: hasEmail={}", hasEmail(claims.get("email", String.class)));

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", claims.getSubject());
            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", claims.get("email", String.class));
            Map<String, Object> profile = new HashMap<>();
            profile.put("nickname", claims.get("nickname", String.class));
            kakaoAccount.put("profile", profile);
            attributes.put("kakao_account", kakaoAccount);

            return OAuthAttributes.of("kakao", attributes);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao ID Token 검증 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * 카카오에서 온 사용자 정보를 기반으로 정보를 매핑합니다.
     * <p>
     * 카카오에서 응답 온 형식을 그대로 사용합니다.
     */
    private Map<String, Object> buildAttributesMap(KakaoUserResponse response) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", response.id());

        if (response.kakaoAccount() != null) {
            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", response.kakaoAccount().email());

            if (response.kakaoAccount().profile() != null) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("nickname", response.kakaoAccount().profile().nickname());
                kakaoAccount.put("profile", profile);
            }

            attributes.put("kakao_account", kakaoAccount);
        }

        return attributes;
    }

    /**
     * Kakao Access Token을 사용하여 앱과 사용자의 연결을 해제합니다.
     *
     * @param accessToken Kakao Access Token
     * @see <a href="https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#unlink">Kakao 연결 끊기</a>
     */
    public void unlinkUser(String accessToken) {
        log.info("Kakao 사용자 연결 끊기 시작");

        try {
            ExternalApiCallLogger.measure("KAKAO", "UNLINK_USER", () ->
                restClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao 연결 끊기 실패: status={}", res.getStatusCode());
                        throw new AuthenticationDomainException(
                            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
                    })
                    .toBodilessEntity()
            );

            log.info("Kakao 연결을 끊었습니다");

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao 연결 끊기 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Kakao Admin API를 사용하여 앱과 사용자의 연결을 해제합니다.
     *
     * @param kakaoUserId Kakao 사용자 고유 ID (providerId)
     * @see <a href="https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#unlink">Kakao 연결 끊기</a>
     */
    public void unlinkUserByAdmin(String kakaoUserId) {
        log.info("Kakao 사용자 연결 끊기 시작: targetProvided={}", hasText(kakaoUserId));

        if (!hasText(kakaoOAuthProperties.adminKey())) {
            log.warn("Kakao Admin Key가 없어 연결 끊기를 건너뜁니다: targetProvided={}", hasText(kakaoUserId));
            return;
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("target_id_type", "user_id");
            formData.add("target_id", kakaoUserId);

            ExternalApiCallLogger.measure("KAKAO", "ADMIN_UNLINK_USER", () ->
                restClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header("Authorization", "KakaoAK " + kakaoOAuthProperties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao 연결 끊기 실패: status={}, targetProvided={}",
                            res.getStatusCode(), hasText(kakaoUserId));
                        throw new AuthenticationDomainException(
                            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
                    })
                    .toBodilessEntity()
            );

            log.info("Kakao 연결을 끊었습니다: targetProvided={}", hasText(kakaoUserId));

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao 연결 끊기 실패: targetProvided={}", hasText(kakaoUserId), e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    private boolean isJwt(String token) {
        return StringUtils.hasText(token) && token.split("\\.").length == 3;
    }

    private OidcJwksSpec kakaoJwksSpec() {
        return new OidcJwksSpec(
            CacheNamespace.KAKAO_JWKS,
            KAKAO_JWKS_URL,
            kakaoOAuthProperties.jwksCache().ttl(),
            kakaoOAuthProperties.jwksCache().maxSize()
        );
    }

    private boolean hasEmail(KakaoUserResponse response) {
        return response.kakaoAccount() != null && hasText(response.kakaoAccount().email());
    }

    private boolean hasEmail(String email) {
        return hasText(email);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // ===== Response DTOs =====

    private record KakaoTokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        Integer expiresIn,
        String scope,
        @JsonProperty("id_token")
        String idToken
    ) {
    }

    private record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
        String email,
        Profile profile
    ) {
    }

    private record Profile(
        String nickname
    ) {
    }
}
