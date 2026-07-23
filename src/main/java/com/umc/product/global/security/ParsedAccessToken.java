package com.umc.product.global.security;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;

public record ParsedAccessToken(
    Long memberId,
    List<String> roles,
    ClientType clientType,
    ClientContextClaims clientContextClaims,
    boolean requiredTermsAgreed,
    Instant expiresAt
) {

    public ParsedAccessToken {
        roles = roles == null ? List.of() : List.copyOf(roles);
        clientContextClaims = clientContextClaims == null ? ClientContextClaims.empty() : clientContextClaims;
    }

    public ParsedAccessToken(Long memberId, List<String> roles, ClientType clientType) {
        this(memberId, roles, clientType, ClientContextClaims.empty(), true, null);
    }
}
