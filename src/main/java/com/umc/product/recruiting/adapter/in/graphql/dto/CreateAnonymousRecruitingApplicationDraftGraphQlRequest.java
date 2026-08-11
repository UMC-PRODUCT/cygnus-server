package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;

public record CreateAnonymousRecruitingApplicationDraftGraphQlRequest(
    String applicationFormId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    String privacyTermId,
    boolean privacyAgreed
) {

    public CreateAnonymousRecruitingApplicationDraftCommand toCommand() {
        return CreateAnonymousRecruitingApplicationDraftCommand.builder()
            .applicationFormId(GlobalId.decodeLong(applicationFormId, GlobalIdTypes.RECRUITING_APPLICATION_FORM))
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .privacyTermId(GlobalId.decodeLong(privacyTermId, GlobalIdTypes.PRIVACY_TERM))
            .privacyAgreed(privacyAgreed)
            .build();
    }
}
