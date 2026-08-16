package com.umc.product.authentication.application.port.in.command.dto;

public record LoginSsoBrowserByEmailCommand(
    String email,
    String rawPassword
) {
    public static LoginSsoBrowserByEmailCommand of(String email, String rawPassword) {
        return new LoginSsoBrowserByEmailCommand(email, rawPassword);
    }

    @Override
    public String toString() {
        return "LoginSsoBrowserByEmailCommand[email=" + email + ", rawPassword=***]";
    }
}
