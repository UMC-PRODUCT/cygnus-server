package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record CloneRecruitingRoundGraphQlRequest(
    Long targetSeasonId,
    String title,
    RecruitingRoundType type,
    Integer roundNo
) {

    public CloneRecruitingRoundCommand toCommand(
        Long sourceSeasonId,
        Long sourceRoundId,
        Long requesterMemberId
    ) {
        return CloneRecruitingRoundCommand.builder()
            .sourceSeasonId(sourceSeasonId)
            .sourceRoundId(sourceRoundId)
            .targetSeasonId(targetSeasonId)
            .title(title)
            .type(type)
            .roundNo(roundNo)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
