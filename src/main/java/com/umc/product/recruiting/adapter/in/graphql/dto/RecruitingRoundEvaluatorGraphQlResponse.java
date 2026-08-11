package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;

public record RecruitingRoundEvaluatorGraphQlResponse(
    String evaluatorId,
    String roundId,
    String evaluatorMemberId
) {

    public static RecruitingRoundEvaluatorGraphQlResponse from(RecruitingRoundEvaluatorInfo info) {
        return new RecruitingRoundEvaluatorGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND_EVALUATOR, info.id()),
            GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, info.roundId()),
            GlobalId.encode(GlobalIdTypes.MEMBER, info.memberId())
        );
    }
}
