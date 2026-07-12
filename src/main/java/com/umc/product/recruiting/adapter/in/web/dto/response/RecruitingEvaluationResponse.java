package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단계별 지원서 평가 응답")
public record RecruitingEvaluationResponse(
    @Schema(description = "평가 ID", example = "7") Long id,
    @Schema(description = "지원서 ID", example = "40") Long applicationId,
    @Schema(description = "평가자 회원 ID", example = "99") Long evaluatorMemberId,
    @Schema(description = "평가 단계", example = "DOCUMENT") RecruitingEvaluatorStage stage,
    @Schema(description = "평가 상태", example = "SUBMITTED") RecruitingApplicationEvaluationStatus status,
    @Schema(description = "평가 결정", example = "PASS") RecruitingApplicationEvaluationDecision decision,
    @Schema(description = "평가 의견", example = "평가 기준을 충족합니다.") String comment,
    @Schema(description = "평가 제출 시각") Instant submittedAt
) {

    public static RecruitingEvaluationResponse from(RecruitingApplicationEvaluationInfo info) {
        return new RecruitingEvaluationResponse(
            info.id(),
            info.applicationId(),
            info.evaluatorMemberId(),
            info.stage(),
            info.status(),
            info.decision(),
            info.comment(),
            info.submittedAt()
        );
    }
}
