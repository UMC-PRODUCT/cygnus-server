package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AssignRecruitingInterviewCommand(
    Long applicationId,
    Long interviewerMemberId,
    Instant startsAt,
    Instant endsAt,
    String location,
    Long assignedByMemberId
) {
}
