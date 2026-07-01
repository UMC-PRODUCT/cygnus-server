package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateRecruitingSeasonStatusRequest(
    @NotNull RecruitingSeasonStatus status
) {

    public UpdateRecruitingSeasonStatusCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonStatusCommand.builder()
            .seasonId(seasonId)
            .status(status)
            .build();
    }
}
