package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "활성 면접 질문 응답")
public record RecruitingInterviewQuestionResponse(
    @Schema(description = "면접 질문 ID", example = "1") Long id,
    @Schema(description = "모집 차수 ID. 공통 질문에만 존재합니다.", example = "20") Long roundId,
    @Schema(description = "지원서 ID. 개별 질문에만 존재합니다.", example = "40") Long applicationId,
    @Schema(description = "질문 본문", example = "지원 동기를 설명해 주세요.") String content,
    @Schema(description = "질문 노출 순서", example = "0") Integer orderNo,
    @Schema(description = "활성 여부", example = "true") boolean active
) {

    public static RecruitingInterviewQuestionResponse from(RecruitingRoundInterviewQuestionInfo info) {
        return new RecruitingInterviewQuestionResponse(
            info.id(),
            info.roundId(),
            null,
            info.content(),
            info.orderNo(),
            info.active()
        );
    }

    public static RecruitingInterviewQuestionResponse from(RecruitingApplicationInterviewQuestionInfo info) {
        return new RecruitingInterviewQuestionResponse(
            info.id(),
            null,
            info.applicationId(),
            info.content(),
            info.orderNo(),
            info.active()
        );
    }
}
