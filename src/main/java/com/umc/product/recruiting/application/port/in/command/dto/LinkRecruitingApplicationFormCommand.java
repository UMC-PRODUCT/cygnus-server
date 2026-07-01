package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record LinkRecruitingApplicationFormCommand(
    Long roundId,
    Long formId,
    ChallengerTrack track
) {
}
