package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 모집 차수 평가자 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingRoundEvaluatorIdGraphQlPayload(String evaluatorId) {

    public static RecruitingRoundEvaluatorIdGraphQlPayload of(Long rawEvaluatorId) {
        return new RecruitingRoundEvaluatorIdGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND_EVALUATOR, rawEvaluatorId)
        );
    }
}
