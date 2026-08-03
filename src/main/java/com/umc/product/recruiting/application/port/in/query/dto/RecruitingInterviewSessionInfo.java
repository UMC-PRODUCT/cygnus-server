package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record RecruitingInterviewSessionInfo(
    Long id,
    Long roundId,
    String name,
    Instant startsAt,
    Instant endsAt,
    Integer slotDurationMinutes,
    RecruitingInterviewMode mode,
    String location
) {

    public static RecruitingInterviewSessionInfo from(RecruitingInterviewSession session) {
        return new RecruitingInterviewSessionInfo(
            session.getId(),
            session.getRoundId(),
            session.getName(),
            session.getStartsAt(),
            session.getEndsAt(),
            session.getSlotDurationMinutes(),
            session.getMode(),
            session.getLocation()
        );
    }
}
