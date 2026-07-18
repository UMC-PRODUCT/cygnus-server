package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingApplicationEvaluationInfo(
    Long id,
    Long applicationId,
    Long evaluatorMemberId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment,
    Instant submittedAt
) {

    public static RecruitingApplicationEvaluationInfo from(RecruitingApplicationEvaluation evaluation) {
        return new RecruitingApplicationEvaluationInfo(
            evaluation.getId(),
            evaluation.getApplication().getId(),
            evaluation.getEvaluatorMemberId(),
            evaluation.getStage(),
            evaluation.getDecision(),
            evaluation.getComment(),
            evaluation.getSubmittedAt()
        );
    }
}
