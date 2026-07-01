package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

public record RecruitingInterviewEvaluationInfo(
    Long evaluationId,
    Long evaluatorMemberId,
    RecruitingInterviewEvaluationStatus status,
    Integer score,
    String comment,
    Instant submittedAt
) {

    public static RecruitingInterviewEvaluationInfo from(RecruitingInterviewEvaluation evaluation) {
        return new RecruitingInterviewEvaluationInfo(
            evaluation.getId(),
            evaluation.getEvaluatorMemberId(),
            evaluation.getStatus(),
            evaluation.getScore(),
            evaluation.getComment(),
            evaluation.getSubmittedAt()
        );
    }
}
