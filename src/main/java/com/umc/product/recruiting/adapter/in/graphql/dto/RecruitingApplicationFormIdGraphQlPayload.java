package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 모집 지원폼 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingApplicationFormIdGraphQlPayload(String applicationFormId) {

    public static RecruitingApplicationFormIdGraphQlPayload of(Long rawApplicationFormId) {
        return new RecruitingApplicationFormIdGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION_FORM, rawApplicationFormId)
        );
    }
}
