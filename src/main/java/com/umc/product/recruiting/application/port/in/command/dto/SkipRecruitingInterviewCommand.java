package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record SkipRecruitingInterviewCommand(
    Long applicationId,
    Long skippedByMemberId,
    String reason
) {
}
