package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;

public record DemodayVoteAuthorizationInfo(
    String voteAuthorizationToken,
    DemodayBoothInfo selectedBooth,
    Instant expiresAt
) {
}
