package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 시즌 생성 요청")
public record CreateRecruitingSeasonRequest(
    @Schema(description = "모집 대상 기수 ID", example = "15")
    @NotNull Long gisuId,
    @Schema(description = "모집을 진행하는 학교 ID", example = "10")
    @NotNull Long schoolId
) {

    public CreateRecruitingSeasonCommand toCommand() {
        return CreateRecruitingSeasonCommand.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .build();
    }
}
