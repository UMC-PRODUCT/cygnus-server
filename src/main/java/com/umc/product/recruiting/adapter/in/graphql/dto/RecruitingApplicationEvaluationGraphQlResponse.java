package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingApplicationEvaluationGraphQlResponse(
    Long id,
    Long applicationId,
    Long evaluatorMemberId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment,
    Instant submittedAt
) {

    public static RecruitingApplicationEvaluationGraphQlResponse from(RecruitingApplicationEvaluationInfo info) {
        return new RecruitingApplicationEvaluationGraphQlResponse(
            info.id(),
            info.applicationId(),
            info.evaluatorMemberId(),
            info.stage(),
            info.decision(),
            info.comment(),
            info.submittedAt()
        );
    }
}
