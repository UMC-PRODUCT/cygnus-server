package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;

public record RecruitingEvaluationStatisticsGraphQlRequest(
    String gisuId
) {

    public RecruitingEvaluationStatisticsQuery toQuery(Long requesterMemberId) {
        long decodedGisuId = GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU);
        if (decodedGisuId <= 0) {
            throw new IllegalArgumentException("gisuId는 양수여야 합니다.");
        }
        return RecruitingEvaluationStatisticsQuery.builder()
            .gisuId(decodedGisuId)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
