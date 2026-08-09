package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "모집 시즌 트랙별 목표 인원 전체 교체 요청")
public record ReplaceRecruitingSeasonTrackQuotasRequest(
    @Schema(description = "소속 지부의 학교별 파트 목표 인원 합계", example = "740")
    @NotNull @PositiveOrZero Integer chapterTotalTargetCount,
    @Schema(description = "교체할 트랙별 목표 인원 목록")
    @NotNull List<@Valid RecruitingSeasonTrackQuotaRequest> quotas
) {

    public ReplaceRecruitingSeasonTrackQuotasCommand toCommand(Long seasonId) {
        return ReplaceRecruitingSeasonTrackQuotasCommand.builder()
            .seasonId(seasonId)
            .chapterTotalTargetCount(chapterTotalTargetCount)
            .quotas(quotas.stream().map(RecruitingSeasonTrackQuotaRequest::toCommand).toList())
            .build();
    }
}
