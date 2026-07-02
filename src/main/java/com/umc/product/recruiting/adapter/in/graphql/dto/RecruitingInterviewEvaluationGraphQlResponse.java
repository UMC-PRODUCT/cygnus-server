package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

public record RecruitingInterviewEvaluationGraphQlResponse(
    Long evaluationId,
    Long evaluatorMemberId,
    RecruitingInterviewEvaluationStatus status,
    Integer score,
    String comment,
    String submittedAt
) {

    public static RecruitingInterviewEvaluationGraphQlResponse from(RecruitingInterviewEvaluationInfo info) {
        return new RecruitingInterviewEvaluationGraphQlResponse(
            info.evaluationId(),
            info.evaluatorMemberId(),
            info.status(),
            info.score(),
            info.comment(),
            info.submittedAt() == null ? null : info.submittedAt().toString()
        );
    }
}
