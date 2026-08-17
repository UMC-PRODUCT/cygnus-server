package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;

public record DemodayVoteQrInfo(
    Long pollId,
    String qrValue,
    Instant generatedAt,
    Instant expiresAt
) {
}
