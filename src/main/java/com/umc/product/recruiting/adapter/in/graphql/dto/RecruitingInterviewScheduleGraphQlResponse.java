package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

public record RecruitingInterviewScheduleGraphQlResponse(
    Long id,
    Long applicationId,
    RecruitingInterviewScheduleStatus status,
    Instant startsAt,
    Instant endsAt,
    String location,
    String contactSnapshot
) {

    public static RecruitingInterviewScheduleGraphQlResponse from(RecruitingInterviewScheduleInfo info) {
        return new RecruitingInterviewScheduleGraphQlResponse(
            info.id(),
            info.applicationId(),
            info.status(),
            info.startsAt(),
            info.endsAt(),
            info.location(),
            info.contactSnapshot()
        );
    }
}
