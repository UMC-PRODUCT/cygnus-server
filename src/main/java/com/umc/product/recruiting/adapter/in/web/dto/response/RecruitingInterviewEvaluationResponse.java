package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

public record RecruitingInterviewEvaluationResponse(
    Long evaluationId,
    Long evaluatorMemberId,
    RecruitingInterviewEvaluationStatus status,
    Integer score,
    String comment,
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
