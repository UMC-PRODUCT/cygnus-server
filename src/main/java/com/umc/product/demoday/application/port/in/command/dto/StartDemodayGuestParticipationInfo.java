package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;

public record StartDemodayGuestParticipationInfo(
    String participantToken,
    Instant expiresAt,
    DemodayParticipationInfo participation
) {
}
