package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;

public record AssignRecruitingInterviewGraphQlRequest(
    Long interviewerMemberId,
    String startsAt,
    String endsAt,
    String location
) {

    public AssignRecruitingInterviewCommand toCommand(Long applicationId, Long assignedByMemberId) {
        return AssignRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .interviewerMemberId(interviewerMemberId)
            .startsAt(Instant.parse(startsAt))
            .endsAt(Instant.parse(endsAt))
            .location(location)
            .assignedByMemberId(assignedByMemberId)
            .build();
    }
}
