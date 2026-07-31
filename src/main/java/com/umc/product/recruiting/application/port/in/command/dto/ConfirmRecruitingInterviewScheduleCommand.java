package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

public record ConfirmRecruitingInterviewScheduleCommand(
    Long applicationId,
    Long requesterMemberId,
    Instant startsAt,
    Instant endsAt,
    String location,
    String contactSnapshot
) {

    public static ConfirmRecruitingInterviewScheduleCommand of(
        Long applicationId,
        Long requesterMemberId,
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {
        return new ConfirmRecruitingInterviewScheduleCommand(
            applicationId,
            requesterMemberId,
            startsAt,
            endsAt,
            location,
            contactSnapshot
        );
    }
}
