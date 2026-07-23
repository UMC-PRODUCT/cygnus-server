package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모집 시즌 수정 요청")
public record UpdateRecruitingSeasonRequest(
    @Schema(description = "시즌 내 운영진 공유 메모") String memo
) {

    public UpdateRecruitingSeasonCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonCommand.builder()
            .seasonId(seasonId)
            .memo(memo)
            .build();
    }
}
