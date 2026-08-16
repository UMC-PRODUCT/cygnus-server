package com.umc.product.authentication.adapter.out.external;


import org.springframework.boot.context.properties.ConfigurationProperties;

import com.umc.product.common.domain.enums.ClientType;

@ConfigurationProperties(prefix = "app.oauth2.apple")
public record AppleOAuthProperties(
    String iosClientId,
    String webClientId,
    String teamId,
    String keyId,
    String privateKey,
    OidcJwksCacheProperties jwksCache
) {
    public AppleOAuthProperties {
        if (jwksCache == null) {
            jwksCache = OidcJwksCacheProperties.defaults();
        }
    }

    /**
     * ClientType에 매칭되는 Apple client_id를 반환합니다.
     * <p>
     * Apple Sign-In은 플랫폼별로 다른 client_id를 사용합니다.
     * <ul>
     *     <li>IOS: Native Sign in with Apple → Bundle ID</li>
     *     <li>WEB / ANDROID: Sign in with Apple JS(웹 플로우) → Services ID</li>
     * </ul>
     */
    public String resolveClientId(ClientType clientType) {
        return switch (clientType) {
            case IOS -> iosClientId;
            case WEB, ANDROID -> webClientId;
        };
    }
}
