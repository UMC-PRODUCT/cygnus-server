package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import lombok.Builder;

@Builder
public record CreateRecruitingRoundCommand(
    Long seasonId,
    RecruitingRoundType type,
    Integer roundNo
) {
}
