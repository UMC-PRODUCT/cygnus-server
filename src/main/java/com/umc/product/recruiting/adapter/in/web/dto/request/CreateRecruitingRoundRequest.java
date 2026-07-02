package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 차수 생성 요청")
public record CreateRecruitingRoundRequest(
    @Schema(description = "모집 차수 유형", example = "REGULAR")
    @NotNull RecruitingRoundType type,
    @Schema(description = "추가모집 차수 번호. 본모집이면 비워둘 수 있습니다.", example = "1")
    Integer roundNo
) {

    public CreateRecruitingRoundCommand toCommand(Long seasonId) {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .type(type)
            .roundNo(roundNo)
            .build();
    }
}
