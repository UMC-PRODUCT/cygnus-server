package com.umc.product.authentication.adapter.in.web.dto.request;

import org.springframework.util.StringUtils;

import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByOAuthTokenCommand;
import com.umc.product.common.domain.enums.OAuthProvider;

public record SsoOAuthTokenLoginRequest(
    String idToken,
    String accessToken
) {
    public SsoOAuthTokenLoginRequest {
        if (!StringUtils.hasText(idToken) && !StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("idToken 또는 accessToken 중 하나는 필수입니다.");
        }
    }

    public LoginSsoBrowserByOAuthTokenCommand toCommand(OAuthProvider provider) {
        return LoginSsoBrowserByOAuthTokenCommand.of(provider, token());
    }

    private String token() {
        if (StringUtils.hasText(idToken)) {
            return idToken;
        }
        return accessToken;
    }
}
