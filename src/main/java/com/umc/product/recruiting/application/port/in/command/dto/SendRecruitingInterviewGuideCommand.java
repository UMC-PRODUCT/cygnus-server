package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record SendRecruitingInterviewGuideCommand(
    Long applicationId,
    String recipientEmail,
    Instant startsAt,
    String location
) {
}
