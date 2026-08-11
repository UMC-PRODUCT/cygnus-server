package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;

public record RecruitingRoundEvaluatorGraphQlRequest(
    String seasonId,
    String roundId,
    String evaluatorMemberId
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public Long decodedRoundId() {
        return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
    }

    public RecruitingRoundEvaluatorCommand toCommand(Long requesterMemberId) {
        return RecruitingRoundEvaluatorCommand.of(
            decodedRoundId(),
            requesterMemberId,
            GlobalId.decodeLong(evaluatorMemberId, GlobalIdTypes.MEMBER)
        );
    }
}
