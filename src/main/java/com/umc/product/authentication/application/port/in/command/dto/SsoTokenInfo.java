package com.umc.product.authentication.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.OAuthProvider;

public record SsoTokenInfo(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    String tokenType,
    SsoMemberInfo member,
    List<OAuthProvider> linkedOAuthProviders
) {
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public SsoTokenInfo {
        linkedOAuthProviders = linkedOAuthProviders == null ? List.of() : List.copyOf(linkedOAuthProviders);
    }

    public static SsoTokenInfo of(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        SsoMemberInfo member,
        List<OAuthProvider> linkedOAuthProviders
    ) {
        return new SsoTokenInfo(
            accessToken,
            refreshToken,
            expiresIn,
            BEARER_TOKEN_TYPE,
            member,
            linkedOAuthProviders
        );
    }
}
