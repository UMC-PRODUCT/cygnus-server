package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record CreateAnonymousRecruitingApplicationDraftCommand(
    Long applicationFormId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    Long privacyTermId,
    boolean privacyAgreed
) {
}
