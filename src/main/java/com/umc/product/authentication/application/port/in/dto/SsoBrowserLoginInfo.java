package com.umc.product.authentication.application.port.in.dto;

import java.time.Instant;

public record SsoBrowserLoginInfo(
    Long memberId,
    String loginToken,
    Instant expiresAt
) {
    public static SsoBrowserLoginInfo of(Long memberId, String loginToken, Instant expiresAt) {
        return new SsoBrowserLoginInfo(memberId, loginToken, expiresAt);
    }
}
