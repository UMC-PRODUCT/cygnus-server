package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

public record AddRecruitingFormSectionPolicyGraphQlRequest(
    Long formSectionId,
    RecruitingFormSectionType type,
    ChallengerTrack track
) {

    public AddRecruitingFormSectionPolicyCommand toCommand(Long applicationFormId) {
        return AddRecruitingFormSectionPolicyCommand.builder()
            .applicationFormId(applicationFormId)
            .formSectionId(formSectionId)
            .type(type)
            .track(track)
            .build();
    }
}
