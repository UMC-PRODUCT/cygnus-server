package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record CreateRecruitingInterviewSessionCommand(
    Long roundId,
    Long requesterMemberId,
    String name,
    Instant startsAt,
    Instant endsAt,
    Integer slotDurationMinutes,
    RecruitingInterviewMode mode,
    String location
) {

    public static CreateRecruitingInterviewSessionCommand of(
        Long roundId,
        Long requesterMemberId,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {
        return new CreateRecruitingInterviewSessionCommand(
            roundId, requesterMemberId, name, startsAt, endsAt, slotDurationMinutes, mode, location
        );
    }
}
