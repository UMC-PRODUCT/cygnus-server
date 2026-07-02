package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원 폼 연결 요청")
public record LinkRecruitingApplicationFormRequest(
    @Schema(description = "form 엔진의 폼 ID", example = "500")
    @NotNull Long formId,
    @Schema(description = "지원 폼이 모집하는 챌린저 track", example = "WEB_PRODUCT_ENGINEER")
    @NotNull ChallengerTrack track
) {

    public LinkRecruitingApplicationFormCommand toCommand(Long roundId) {
        return LinkRecruitingApplicationFormCommand.builder()
            .roundId(roundId)
            .formId(formId)
            .track(track)
            .build();
    }
}
