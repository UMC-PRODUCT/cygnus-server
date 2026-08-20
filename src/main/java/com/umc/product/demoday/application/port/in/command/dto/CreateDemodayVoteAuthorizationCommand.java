package com.umc.product.demoday.application.port.in.command.dto;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public record CreateDemodayVoteAuthorizationCommand(
    Long pollId,
    Long boothId,
    String qrToken,
    DemodayParticipant participant
) {
}
