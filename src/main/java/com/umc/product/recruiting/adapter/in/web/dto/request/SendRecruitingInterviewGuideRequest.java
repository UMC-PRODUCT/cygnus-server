package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendRecruitingInterviewGuideRequest(
    @NotBlank String recipientEmail,
    @NotNull Instant startsAt,
    String location
) {

    public SendRecruitingInterviewGuideCommand toCommand(Long applicationId) {
        return SendRecruitingInterviewGuideCommand.builder()
            .applicationId(applicationId)
            .recipientEmail(recipientEmail)
            .startsAt(startsAt)
            .location(location)
            .build();
    }
}
