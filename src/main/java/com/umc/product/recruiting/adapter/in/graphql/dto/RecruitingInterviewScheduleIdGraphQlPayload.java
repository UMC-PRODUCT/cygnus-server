package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 면접 일정 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingInterviewScheduleIdGraphQlPayload(String scheduleId) {

    public static RecruitingInterviewScheduleIdGraphQlPayload of(Long rawScheduleId) {
        return new RecruitingInterviewScheduleIdGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_SCHEDULE, rawScheduleId)
        );
    }
}
