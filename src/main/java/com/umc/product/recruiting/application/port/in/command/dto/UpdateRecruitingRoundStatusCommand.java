package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

import lombok.Builder;

@Builder
public record UpdateRecruitingRoundStatusCommand(
    Long roundId,
    RecruitingRoundStatus status
) {
}
