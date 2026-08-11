package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record CloneRecruitingRoundGraphQlRequest(
    String seasonId,
    String roundId,
    String targetSeasonId,
    String title,
    RecruitingRoundType type,
    Integer roundNo
) {

    public CloneRecruitingRoundCommand toCommand(Long requesterMemberId) {
        return CloneRecruitingRoundCommand.builder()
            .sourceSeasonId(GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON))
            .sourceRoundId(GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND))
            .targetSeasonId(GlobalId.decodeLong(targetSeasonId, GlobalIdTypes.RECRUITING_SEASON))
            .title(title)
            .type(type)
            .roundNo(roundNo)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
