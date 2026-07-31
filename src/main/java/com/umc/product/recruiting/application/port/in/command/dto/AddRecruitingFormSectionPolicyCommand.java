package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import lombok.Builder;

@Builder
public record AddRecruitingFormSectionPolicyCommand(
    Long applicationFormId,
    Long formSectionId,
    RecruitingFormSectionType type,
    ChallengerTrack track
) {
}
