package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;

public record DemodayAdminVoteInfo(
    Long voteId,
    Instant votedAt,
    ParticipantInfo participant,
    DemodayBoothInfo booth,
    DemodayVoteStatus status,
    Instant revokedAt
) {

    public record ParticipantInfo(
        DemodayParticipantType type,
        String displayName
    ) {
    }
}
