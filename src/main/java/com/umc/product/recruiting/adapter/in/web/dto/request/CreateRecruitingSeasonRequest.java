package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 시즌 생성 요청")
public record CreateRecruitingSeasonRequest(
    @Schema(description = "모집 대상 기수 ID", example = "15")
    @NotNull Long gisuId,
    @Schema(description = "모집을 진행하는 학교 ID", example = "10")
    @NotNull Long schoolId,
    @Schema(description = "초기 트랙별 목표 인원 목록")
    List<@jakarta.validation.Valid RecruitingSeasonTrackQuotaRequest> quotas
) {

    public CreateRecruitingSeasonCommand toCommand(Long requesterMemberId) {
        return CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(requesterMemberId)
            .gisuId(gisuId)
            .schoolId(schoolId)
            .quotas(quotas == null
                ? List.of()
                : quotas.stream().map(RecruitingSeasonTrackQuotaRequest::toCommand).toList())
            .build();
    }
}
