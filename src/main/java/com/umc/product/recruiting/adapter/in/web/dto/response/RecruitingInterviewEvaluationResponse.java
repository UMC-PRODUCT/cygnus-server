package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접 평가 응답")
public record RecruitingInterviewEvaluationResponse(
    @Schema(description = "면접 평가 ID", example = "77")
    Long evaluationId,
    @Schema(description = "평가를 작성한 면접관 회원 ID", example = "1001")
    Long evaluatorMemberId,
    @Schema(description = "면접 평가 제출 상태", example = "SUBMITTED")
    RecruitingInterviewEvaluationStatus status,
    @Schema(description = "지원자 평가 점수", example = "5")
    Integer score,
    @Schema(description = "면접관 평가 코멘트", example = "문제 해결 과정이 명확했습니다.")
    String comment,
    @Schema(description = "평가 제출 일시", example = "2026-07-10T12:00:00Z")
    Instant submittedAt
) {

    public static RecruitingInterviewEvaluationResponse from(RecruitingInterviewEvaluationInfo info) {
        return new RecruitingInterviewEvaluationResponse(
            info.evaluationId(),
            info.evaluatorMemberId(),
            info.status(),
            info.score(),
            info.comment(),
            info.submittedAt()
        );
    }
}
