package com.umc.product.recruiting.application.port.in.command.dto;

public record RequestRecruitingInterviewScheduleCommand(
    Long applicationId,
    Long requesterMemberId,
    String contactSnapshot
) {

    public static RequestRecruitingInterviewScheduleCommand of(
        Long applicationId,
        Long requesterMemberId,
        String contactSnapshot
    ) {
        return new RequestRecruitingInterviewScheduleCommand(applicationId, requesterMemberId, contactSnapshot);
    }
}
