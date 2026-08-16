package com.umc.product.authentication.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;

public record SsoBrowserLoginResponse(
    Long memberId,
    Instant expiresAt
) {
    public static SsoBrowserLoginResponse from(SsoBrowserLoginInfo info) {
        return new SsoBrowserLoginResponse(info.memberId(), info.expiresAt());
    }
}
