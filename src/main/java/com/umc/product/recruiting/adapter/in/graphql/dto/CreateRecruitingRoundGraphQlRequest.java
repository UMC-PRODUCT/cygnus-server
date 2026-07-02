package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record CreateRecruitingRoundGraphQlRequest(
    RecruitingRoundType type,
    Integer roundNo
) {

    public CreateRecruitingRoundCommand toCommand(Long seasonId) {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .type(type)
            .roundNo(roundNo)
            .build();
    }
}
