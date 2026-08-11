package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 모집 차수 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingRoundIdGraphQlPayload(String roundId) {

    public static RecruitingRoundIdGraphQlPayload of(Long rawRoundId) {
        return new RecruitingRoundIdGraphQlPayload(GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, rawRoundId));
    }
}
