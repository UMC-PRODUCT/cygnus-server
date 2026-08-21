package com.umc.product.demoday.application.port.out;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;

public record DemodayVoteAuthorizationTokenClaims(
    String purpose,
    DemodayParticipantType participantType,
    Long participantId,
    Long pollId,
    Long boothId,
    Instant issuedAt,
    Instant expiresAt
) {
}
