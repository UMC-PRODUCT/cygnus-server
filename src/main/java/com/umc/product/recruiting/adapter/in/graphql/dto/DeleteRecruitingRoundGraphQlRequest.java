package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;

public record DeleteRecruitingRoundGraphQlRequest(
    String seasonId,
    String roundId
) {

    public DeleteRecruitingRoundCommand toCommand(Long requesterMemberId) {
        return DeleteRecruitingRoundCommand.builder()
            .seasonId(GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON))
            .roundId(GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND))
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
