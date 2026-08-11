package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 등록 준비/취소/확정 뮤테이션 공용 입력. 세 input 스키마 타입 모두에 매핑된다.
 */
public record RecruitingRegistrationGraphQlRequest(
    String seasonId,
    String applicationId
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public Long decodedApplicationId() {
        return GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION);
    }
}
