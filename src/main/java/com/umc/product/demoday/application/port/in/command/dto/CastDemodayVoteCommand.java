package com.umc.product.demoday.application.port.in.command.dto;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public record CastDemodayVoteCommand(
    Long pollId,
    String voteAuthorizationToken,
    DemodayParticipant participant
) {
}
