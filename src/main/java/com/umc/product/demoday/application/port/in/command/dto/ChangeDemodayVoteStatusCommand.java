package com.umc.product.demoday.application.port.in.command.dto;

import java.util.Objects;

public record ChangeDemodayVoteStatusCommand(
    Long pollId,
    Long voteId,
    Long requesterMemberId,
    String reason
) {

    public ChangeDemodayVoteStatusCommand {
        Objects.requireNonNull(pollId, "pollId must not be null");
        Objects.requireNonNull(voteId, "voteId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        reason = reason.strip();
    }
}
