package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.Set;

public record SetRecruitingRoundEvaluatorsCommand(
    Long roundId,
    Long requesterMemberId,
    Set<Long> memberIds
) {

    public SetRecruitingRoundEvaluatorsCommand {
        memberIds = memberIds == null ? Set.of() : Set.copyOf(memberIds);
    }
}
