package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 모집 시즌 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingSeasonIdGraphQlPayload(String seasonId) {

    public static RecruitingSeasonIdGraphQlPayload of(Long rawSeasonId) {
        return new RecruitingSeasonIdGraphQlPayload(GlobalId.encode(GlobalIdTypes.RECRUITING_SEASON, rawSeasonId));
    }
}
