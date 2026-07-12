package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record LinkRecruitingApplicationFormCommand(
    Long seasonId,
    Long roundId,
    Long formId
) {
}
