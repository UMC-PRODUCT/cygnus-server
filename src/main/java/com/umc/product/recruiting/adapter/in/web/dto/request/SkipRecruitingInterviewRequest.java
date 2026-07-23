package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 생략 요청")
public record SkipRecruitingInterviewRequest(
    @Schema(description = "면접 생략 사유 또는 운영진 메모", example = "학교 정책상 면접 전형을 진행하지 않습니다.", maxLength = 255)
    @Size(max = 255) String reason
) {

    public SkipRecruitingInterviewCommand toCommand(Long applicationId, Long skippedByMemberId) {
        return SkipRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .skippedByMemberId(skippedByMemberId)
            .reason(reason)
            .build();
    }
}
