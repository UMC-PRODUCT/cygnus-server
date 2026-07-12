package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 차수 상태 변경 요청")
public record UpdateRecruitingRoundStatusRequest(
    @Schema(description = "변경할 모집 차수 상태", example = "OPEN")
    @NotNull RecruitingRoundStatus status
) {

    public UpdateRecruitingRoundStatusCommand toCommand(Long seasonId, Long roundId) {
        return UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .status(status)
            .build();
    }
}
