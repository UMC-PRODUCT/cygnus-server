package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 면접 세션 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingInterviewSessionIdGraphQlPayload(String sessionId) {

    public static RecruitingInterviewSessionIdGraphQlPayload of(Long rawSessionId) {
        return new RecruitingInterviewSessionIdGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_SESSION, rawSessionId)
        );
    }
}
