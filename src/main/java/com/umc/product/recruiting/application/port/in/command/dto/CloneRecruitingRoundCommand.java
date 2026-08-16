package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import lombok.Builder;

@Builder
public record CloneRecruitingRoundCommand(
    Long sourceSeasonId,
    Long sourceRoundId,
    Long targetSeasonId,
    String title,
    RecruitingRoundType type,
    Integer roundNo,
    Long requesterMemberId
) {
}
