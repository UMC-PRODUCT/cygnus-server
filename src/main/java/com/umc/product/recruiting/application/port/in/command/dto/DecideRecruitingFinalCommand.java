package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record DecideRecruitingFinalCommand(
    Long applicationId,
    RecruitingDecisionStatus decision,
    ChallengerTrack acceptedTrack,
    Long decidedByMemberId,
    String reason
) {
}
