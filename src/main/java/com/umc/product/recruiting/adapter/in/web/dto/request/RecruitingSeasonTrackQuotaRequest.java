package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingSeasonTrackQuotaCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "모집 시즌의 트랙별 목표 인원")
public record RecruitingSeasonTrackQuotaRequest(
    @Schema(description = "모집 트랙", example = "PLAN") @NotNull ChallengerTrack track,
    @Schema(description = "목표 인원", example = "5") @NotNull @PositiveOrZero Integer targetCount
) {

    public RecruitingSeasonTrackQuotaCommand toCommand() {
        return RecruitingSeasonTrackQuotaCommand.of(track, targetCount);
    }
}
