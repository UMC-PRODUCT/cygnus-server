package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

public record UpdateRecruitingSeasonStatusGraphQlRequest(
    RecruitingSeasonStatus status
) {

    public UpdateRecruitingSeasonStatusCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonStatusCommand.builder()
            .seasonId(seasonId)
            .status(status)
            .build();
    }
}
