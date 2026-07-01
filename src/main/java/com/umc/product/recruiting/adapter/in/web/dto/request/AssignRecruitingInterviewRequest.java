package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;

import jakarta.validation.constraints.NotNull;

public record AssignRecruitingInterviewRequest(
    @NotNull Long interviewerMemberId,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    String location
) {

    public AssignRecruitingInterviewCommand toCommand(Long applicationId, Long assignedByMemberId) {
        return AssignRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .interviewerMemberId(interviewerMemberId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .location(location)
            .assignedByMemberId(assignedByMemberId)
            .build();
    }
}
