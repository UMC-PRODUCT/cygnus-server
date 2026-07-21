package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand.AnswerEntry;

import lombok.Builder;

@Builder
public record UpdateAnonymousRecruitingApplicationCommand(
    String credentialEmail,
    String applicationKey,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    List<AnswerEntry> answers
) {
}
