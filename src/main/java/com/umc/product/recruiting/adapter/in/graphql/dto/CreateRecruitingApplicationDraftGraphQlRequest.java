package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;

public record CreateRecruitingApplicationDraftGraphQlRequest(
    String applicationFormId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice
) {

    public CreateRecruitingApplicationDraftCommand toCommand(Long applicantMemberId) {
        return CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(GlobalId.decodeLong(applicationFormId, GlobalIdTypes.RECRUITING_APPLICATION_FORM))
            .applicantMemberId(applicantMemberId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .build();
    }
}
