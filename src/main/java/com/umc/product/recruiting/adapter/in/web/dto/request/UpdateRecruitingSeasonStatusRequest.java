package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 시즌 상태 변경 요청")
public record UpdateRecruitingSeasonStatusRequest(
    @Schema(description = "변경할 모집 시즌 상태", example = "OPEN")
    @NotNull RecruitingSeasonStatus status
) {

    public UpdateRecruitingSeasonStatusCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonStatusCommand.builder()
            .seasonId(seasonId)
            .status(status)
            .build();
    }
}
