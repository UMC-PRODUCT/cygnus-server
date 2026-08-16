package com.umc.product.authentication.application.port.in.command.dto;

import java.util.Objects;

import org.springframework.util.StringUtils;

import com.umc.product.common.domain.enums.OAuthProvider;

public record LoginSsoBrowserByOAuthTokenCommand(
    OAuthProvider provider,
    String token
) {
    public LoginSsoBrowserByOAuthTokenCommand {
        Objects.requireNonNull(provider, "provider must not be null");
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token must not be blank");
        }
    }

    public static LoginSsoBrowserByOAuthTokenCommand of(OAuthProvider provider, String token) {
        return new LoginSsoBrowserByOAuthTokenCommand(provider, token);
    }
}
