package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import lombok.Builder;

@Builder
public record UpdateRecruitingSeasonStatusCommand(
    Long seasonId,
    RecruitingSeasonStatus status
) {
}
