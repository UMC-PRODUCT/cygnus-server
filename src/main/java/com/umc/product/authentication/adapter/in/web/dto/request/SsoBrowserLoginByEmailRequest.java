package com.umc.product.authentication.adapter.in.web.dto.request;

import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByEmailCommand;

import jakarta.validation.constraints.NotBlank;

public record SsoBrowserLoginByEmailRequest(
    @NotBlank String email,
    @NotBlank String password
) {
    public LoginSsoBrowserByEmailCommand toCommand() {
        return LoginSsoBrowserByEmailCommand.of(email, password);
    }

    @Override
    public String toString() {
        return "SsoBrowserLoginByEmailRequest[email=" + email + ", password=***]";
    }
}
