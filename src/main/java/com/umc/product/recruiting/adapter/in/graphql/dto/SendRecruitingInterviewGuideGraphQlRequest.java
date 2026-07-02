package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;

public record SendRecruitingInterviewGuideGraphQlRequest(
    String recipientEmail,
    String startsAt,
    String location
) {

    public SendRecruitingInterviewGuideCommand toCommand(Long applicationId) {
        return SendRecruitingInterviewGuideCommand.builder()
            .applicationId(applicationId)
            .recipientEmail(recipientEmail)
            .startsAt(Instant.parse(startsAt))
            .location(location)
            .build();
    }
}
