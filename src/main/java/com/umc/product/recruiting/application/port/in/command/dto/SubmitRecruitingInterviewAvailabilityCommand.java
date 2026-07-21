package com.umc.product.recruiting.application.port.in.command.dto;

public record SubmitRecruitingInterviewAvailabilityCommand(
    Long applicationId,
    Long requesterMemberId
) {

    public static SubmitRecruitingInterviewAvailabilityCommand of(
        Long applicationId,
        Long requesterMemberId
    ) {
        return new SubmitRecruitingInterviewAvailabilityCommand(
            applicationId,
            requesterMemberId
        );
    }
}
