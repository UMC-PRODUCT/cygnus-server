package com.umc.product.demoday.application.port.in.command.dto;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public record CollectDemodayStampCommand(
    Long pollId,
    String qrCredential,
    DemodayParticipant participant
) {
}
