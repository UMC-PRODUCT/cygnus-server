package com.umc.product.authentication.adapter.out.external;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kakao OAuth 설정값.
 * <p>
 * Authorization Code 흐름에서 token endpoint 호출과 redirect URI 검증에 사용합니다.
 * Spring Security OAuth2 자동 구성용 `spring.security.oauth2.client.registration.kakao`와는 별개로,
 * 직접 token endpoint를 호출하기 위해 필요한 값을 명시적으로 관리합니다.
 */
@ConfigurationProperties(prefix = "app.oauth2.kakao")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    String adminKey,
    List<String> allowedRedirectUris,
    OidcJwksCacheProperties jwksCache
) {
    public KakaoOAuthProperties {
        if (allowedRedirectUris == null) {
            allowedRedirectUris = List.of();
        }
        if (jwksCache == null) {
            jwksCache = OidcJwksCacheProperties.defaults();
        }
    }

    /**
     * 주어진 redirect URI가 화이트리스트에 포함되어 있는지 확인합니다.
     * <p>
     * 공개 클라이언트가 임의 redirect URI를 주입해 Kakao 응답을 가로채는 경우를 차단하기 위한 방어선입니다.
     */
    public boolean isAllowedRedirectUri(String redirectUri) {
        if (redirectUri == null) {
            return false;
        }
        return allowedRedirectUris.contains(redirectUri);
    }
}
