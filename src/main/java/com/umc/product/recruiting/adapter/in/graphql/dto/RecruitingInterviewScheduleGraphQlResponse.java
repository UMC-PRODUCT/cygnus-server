package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

public record RecruitingInterviewScheduleGraphQlResponse(
    String scheduleId,
    String applicationId,
    RecruitingInterviewScheduleStatus status,
    Instant startsAt,
    Instant endsAt,
    String location,
    String contactSnapshot
) {

    public static RecruitingInterviewScheduleGraphQlResponse from(RecruitingInterviewScheduleInfo info) {
        return new RecruitingInterviewScheduleGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_SCHEDULE, info.id()),
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
            info.status(),
            info.startsAt(),
            info.endsAt(),
            info.location(),
            info.contactSnapshot()
        );
    }
}
