package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;

public record RecruitingEvaluationStatisticsGraphQlRequest(
    Long gisuId
) {

    public RecruitingEvaluationStatisticsGraphQlRequest {
        if (gisuId == null || gisuId <= 0) {
            throw new IllegalArgumentException("gisuId는 양수여야 합니다.");
        }
    }

    public RecruitingEvaluationStatisticsQuery toQuery(Long requesterMemberId) {
        return RecruitingEvaluationStatisticsQuery.builder()
            .gisuId(gisuId)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
