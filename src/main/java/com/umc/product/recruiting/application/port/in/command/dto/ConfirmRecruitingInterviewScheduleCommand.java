package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

public record ConfirmRecruitingInterviewScheduleCommand(
    Long applicationId,
    Long requesterMemberId,
    Long sessionId,
    Instant startsAt,
    Instant endsAt,
    String location,
    String contactSnapshot
) {

    public static ConfirmRecruitingInterviewScheduleCommand of(
        Long applicationId,
        Long requesterMemberId,
        Long sessionId,
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {
        return new ConfirmRecruitingInterviewScheduleCommand(
            applicationId,
            requesterMemberId,
            sessionId,
            startsAt,
            endsAt,
            location,
            contactSnapshot
        );
    }

    /**
     * 세션 기반 단건 확정 adapter 전환 전까지 소스 호환성을 유지한다.
     * 세션 없는 명령은 application service에서 거부된다.
     */
    public static ConfirmRecruitingInterviewScheduleCommand of(
        Long applicationId,
        Long requesterMemberId,
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {
        return of(applicationId, requesterMemberId, null, startsAt, endsAt, location, contactSnapshot);
    }
}
