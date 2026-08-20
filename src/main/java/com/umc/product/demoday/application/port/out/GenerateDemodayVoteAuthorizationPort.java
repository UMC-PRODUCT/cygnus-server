package com.umc.product.demoday.application.port.out;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public interface GenerateDemodayVoteAuthorizationPort {

    String generate(
        DemodayParticipant participant,
        Long pollId,
        Long boothId,
        Instant issuedAt,
        Instant expiresAt
    );
}
