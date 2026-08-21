package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;

public record DemodayVoteStatusInfo(
    Long voteId,
    DemodayVoteStatus status,
    Instant revokedAt
) {

    public static DemodayVoteStatusInfo from(DemodayVote vote) {
        return new DemodayVoteStatusInfo(
            vote.getId(),
            vote.isRevoked() ? DemodayVoteStatus.REVOKED : DemodayVoteStatus.VALID,
            vote.getRevokedAt()
        );
    }
}
