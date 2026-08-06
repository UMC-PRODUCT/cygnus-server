package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewSessionInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record RecruitingInterviewSessionGraphQlResponse(
    Long id,
    Long roundId,
    String name,
    Instant startsAt,
    Instant endsAt,
    Integer slotDurationMinutes,
    RecruitingInterviewMode mode,
    String location
) {

    public static RecruitingInterviewSessionGraphQlResponse from(RecruitingInterviewSessionInfo info) {
        return new RecruitingInterviewSessionGraphQlResponse(
            info.id(), info.roundId(), info.name(), info.startsAt(), info.endsAt(), info.slotDurationMinutes(),
            info.mode(), info.location()
        );
    }
}
