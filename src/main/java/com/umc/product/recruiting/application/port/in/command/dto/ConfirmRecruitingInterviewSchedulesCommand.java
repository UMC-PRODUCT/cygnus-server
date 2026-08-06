package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

public record ConfirmRecruitingInterviewSchedulesCommand(
    Long roundId,
    Long requesterMemberId,
    List<Assignment> assignments
) {

    public static final int MAX_ASSIGNMENT_COUNT = 100;

    public ConfirmRecruitingInterviewSchedulesCommand {
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    public static ConfirmRecruitingInterviewSchedulesCommand of(
        Long roundId,
        Long requesterMemberId,
        List<Assignment> assignments
    ) {
        return new ConfirmRecruitingInterviewSchedulesCommand(roundId, requesterMemberId, assignments);
    }

    public record Assignment(
        Long applicationId,
        Long sessionId,
        Instant startsAt,
        String contactSnapshot
    ) {

        public static Assignment of(
            Long applicationId,
            Long sessionId,
            Instant startsAt,
            String contactSnapshot
        ) {
            return new Assignment(applicationId, sessionId, startsAt, contactSnapshot);
        }
    }
}
