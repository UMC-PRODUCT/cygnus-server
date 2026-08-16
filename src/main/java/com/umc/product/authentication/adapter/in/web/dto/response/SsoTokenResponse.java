package com.umc.product.authentication.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.authentication.application.port.in.command.dto.SsoMemberInfo;
import com.umc.product.authentication.application.port.in.command.dto.SsoTokenInfo;
import com.umc.product.common.domain.enums.OAuthProvider;

import lombok.Builder;

@Builder
public record SsoTokenResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    String tokenType,
    SsoMemberInfo member,
    List<OAuthProvider> linkedOAuthProviders
) {
    public static SsoTokenResponse from(SsoTokenInfo info) {
        return SsoTokenResponse.builder()
            .accessToken(info.accessToken())
            .refreshToken(info.refreshToken())
            .expiresIn(info.expiresIn())
            .tokenType(info.tokenType())
            .member(info.member())
            .linkedOAuthProviders(info.linkedOAuthProviders())
            .build();
    }
}
