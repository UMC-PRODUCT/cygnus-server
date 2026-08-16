package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;

public record CreateAnonymousRecruitingApplicationDraftGraphQlRequest(
    Long applicationFormId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    Long privacyTermId,
    boolean privacyAgreed
) {

    public CreateAnonymousRecruitingApplicationDraftCommand toCommand() {
        return CreateAnonymousRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .privacyTermId(privacyTermId)
            .privacyAgreed(privacyAgreed)
            .build();
    }
}
