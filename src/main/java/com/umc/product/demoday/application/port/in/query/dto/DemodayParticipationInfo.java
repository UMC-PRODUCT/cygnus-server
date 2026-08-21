package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;

public record DemodayParticipationInfo(
        Long pollId,
        DemodayParticipantType participantType,
        int stampCount,
        int requiredStampCount,
        List<DemodayStampInfo> stamps,
        Instant nextStampAvailableAt,
        boolean hasActiveVote,
        boolean hasUsedVoteSlot,
        boolean canRequestVoteAuthorization,
        DemodayVoteReceiptInfo activeVoteReceipt
) {
}
