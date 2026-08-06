package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;

public record UpdateRecruitingSeasonGraphQlRequest(String memo) {

    public UpdateRecruitingSeasonCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonCommand.builder()
            .seasonId(seasonId)
            .memo(memo)
            .build();
    }
}
