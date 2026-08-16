package com.umc.product.authentication.application.port.in.command.dto;

import org.springframework.util.StringUtils;

public record LoginSsoBrowserByAppleAuthorizationCodeCommand(
    String authorizationCode
) {
    public LoginSsoBrowserByAppleAuthorizationCodeCommand {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new IllegalArgumentException("authorizationCode must not be blank");
        }
    }

    public static LoginSsoBrowserByAppleAuthorizationCodeCommand from(String authorizationCode) {
        return new LoginSsoBrowserByAppleAuthorizationCodeCommand(authorizationCode);
    }
}
