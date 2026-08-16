package com.umc.product.authentication.application.port.in.command.dto;

import com.umc.product.global.client.ClientContextClaims;

import lombok.Builder;

@Builder
public record NewTokens(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    ClientContextClaims clientContextClaims
) {
}
