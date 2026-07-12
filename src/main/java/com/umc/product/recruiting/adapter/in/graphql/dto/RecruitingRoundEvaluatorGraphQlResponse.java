package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingRoundEvaluatorGraphQlResponse(
    Long id,
    Long roundId,
    Long evaluatorMemberId,
    RecruitingEvaluatorStage stage
) {

    public static RecruitingRoundEvaluatorGraphQlResponse from(RecruitingRoundEvaluatorInfo info) {
        return new RecruitingRoundEvaluatorGraphQlResponse(
            info.id(),
            info.roundId(),
            info.memberId(),
            info.stage()
        );
    }
}
