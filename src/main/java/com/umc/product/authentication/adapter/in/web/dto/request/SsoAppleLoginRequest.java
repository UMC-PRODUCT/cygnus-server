package com.umc.product.authentication.adapter.in.web.dto.request;

import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByAppleAuthorizationCodeCommand;

import jakarta.validation.constraints.NotBlank;

public record SsoAppleLoginRequest(
    @NotBlank String authorizationCode
) {
    public LoginSsoBrowserByAppleAuthorizationCodeCommand toCommand() {
        return LoginSsoBrowserByAppleAuthorizationCodeCommand.from(authorizationCode);
    }
}
