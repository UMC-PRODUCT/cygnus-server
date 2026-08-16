package com.umc.product.authentication.application.port.in.command.dto;

public record ExchangeSsoAuthorizationCodeCommand(
    String grantType,
    String code,
    String clientId,
    String redirectUri,
    String codeVerifier
) {
    public static ExchangeSsoAuthorizationCodeCommand of(
        String grantType,
        String code,
        String clientId,
        String redirectUri,
        String codeVerifier
    ) {
        return new ExchangeSsoAuthorizationCodeCommand(
            grantType,
            code,
            clientId,
            redirectUri,
            codeVerifier
        );
    }
}
