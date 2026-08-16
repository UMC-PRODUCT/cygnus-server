package com.umc.product.authentication.application.service;

import java.time.Instant;

public record SsoLoginTokenClaims(
    Long memberId,
    Instant issuedAt,
    Instant expiresAt,
    String authenticationMethod
) {
    public static SsoLoginTokenClaims of(
        Long memberId,
        Instant issuedAt,
        Instant expiresAt,
        String authenticationMethod
    ) {
        return new SsoLoginTokenClaims(memberId, issuedAt, expiresAt, authenticationMethod);
    }
}
