package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접 스킵 요청")
public record SkipRecruitingInterviewRequest(
    @Schema(description = "면접을 진행하지 않는 사유", example = "학교 정책상 면접 없이 최종 선발합니다.")
    String reason
) {

    public SkipRecruitingInterviewCommand toCommand(Long applicationId, Long skippedByMemberId) {
        return SkipRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .skippedByMemberId(skippedByMemberId)
            .reason(reason)
            .build();
    }
}
