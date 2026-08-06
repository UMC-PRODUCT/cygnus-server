package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

public record SubmitRecruitingInterviewAvailabilityCommand(
    Long applicationId,
    Long requesterMemberId,
    List<Instant> times
) {

    public SubmitRecruitingInterviewAvailabilityCommand {
        times = times == null ? List.of() : List.copyOf(times);
    }

    public static SubmitRecruitingInterviewAvailabilityCommand of(
        Long applicationId,
        Long requesterMemberId,
        List<Instant> times
    ) {
        return new SubmitRecruitingInterviewAvailabilityCommand(
            applicationId,
            requesterMemberId,
            times
        );
    }
}
