package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

public record UpdateRecruitingRoundStatusGraphQlRequest(
    String seasonId,
    String roundId,
    RecruitingRoundStatus status
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public Long decodedRoundId() {
        return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
    }

    public UpdateRecruitingRoundStatusCommand toCommand(Long requesterMemberId) {
        return UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(decodedSeasonId())
            .roundId(decodedRoundId())
            .status(status)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
