package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import jakarta.validation.constraints.NotNull;

public record CreateRecruitingRoundRequest(
    @NotNull RecruitingRoundType type,
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
