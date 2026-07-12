package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record CreateRecruitingApplicationDraftCommand(
    Long applicationFormId,
    Long applicantMemberId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice
) {
}
