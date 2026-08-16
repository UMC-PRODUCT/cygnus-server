package com.umc.product.global.security;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.client.ClientContextClaims;

public record RefreshTokenClaims(
    Long memberId,
    UUID jti,
    Instant expiresAt,
    ClientContextClaims clientContext
) {
    public RefreshTokenClaims(Long memberId, UUID jti, Instant expiresAt) {
        this(memberId, jti, expiresAt, ClientContextClaims.empty());
    }

    public RefreshTokenClaims {
        clientContext = clientContext == null ? ClientContextClaims.empty() : clientContext;
    }
}
