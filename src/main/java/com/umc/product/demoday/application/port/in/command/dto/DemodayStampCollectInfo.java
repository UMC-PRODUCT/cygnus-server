package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;

public record DemodayStampCollectInfo(
    DemodayStampInfo stamp,
    int stampCount,
    int requiredStampCount,
    Instant nextStampAvailableAt,
    boolean canRequestVoteAuthorization
) {
}
