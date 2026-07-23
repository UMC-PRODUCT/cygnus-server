package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record UpdateRecruitingSeasonCommand(
    Long seasonId,
    String memo
) {
}
