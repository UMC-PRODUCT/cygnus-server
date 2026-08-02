package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record UpdateRecruitingInterviewSessionCommand(
    Long sessionId,
    Long roundId,
    Long requesterMemberId,
    String name,
    Instant startsAt,
    Instant endsAt,
    RecruitingInterviewMode mode,
    String location
) {

    public static UpdateRecruitingInterviewSessionCommand of(
        Long sessionId,
        Long roundId,
        Long requesterMemberId,
        String name,
        Instant startsAt,
        Instant endsAt,
        RecruitingInterviewMode mode,
        String location
    ) {
        return new UpdateRecruitingInterviewSessionCommand(
            sessionId, roundId, requesterMemberId, name, startsAt, endsAt, mode, location
        );
    }
}
