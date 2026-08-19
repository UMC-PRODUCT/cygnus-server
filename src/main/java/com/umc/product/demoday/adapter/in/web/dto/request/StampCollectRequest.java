package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.CollectDemodayStampCommand;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

import jakarta.validation.constraints.NotBlank;

public record StampCollectRequest(
    @NotBlank String qrCredential
) {
    public CollectDemodayStampCommand toCommand(Long pollId, DemodayParticipant participant) {
        return new CollectDemodayStampCommand(pollId, qrCredential, participant);
    }
}
