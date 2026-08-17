package com.umc.product.demoday.application.port.out;

import java.time.Instant;

public record DemodayVoteQrTokenClaims(
    String purpose,
    Long pollId,
    Instant issuedAt,
    Instant expiresAt
) {
}
