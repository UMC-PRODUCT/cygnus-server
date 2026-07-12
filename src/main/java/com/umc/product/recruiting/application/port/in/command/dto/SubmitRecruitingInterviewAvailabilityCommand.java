package com.umc.product.recruiting.application.port.in.command.dto;

public record SubmitRecruitingInterviewAvailabilityCommand(
    Long applicationId,
    Long requesterMemberId,
    Long availabilityFormResponseId
) {

    public static SubmitRecruitingInterviewAvailabilityCommand of(
        Long applicationId,
        Long requesterMemberId,
        Long availabilityFormResponseId
    ) {
        return new SubmitRecruitingInterviewAvailabilityCommand(
            applicationId,
            requesterMemberId,
            availabilityFormResponseId
        );
    }
}
