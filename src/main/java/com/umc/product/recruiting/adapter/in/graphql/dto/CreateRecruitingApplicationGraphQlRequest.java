package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;

public record CreateRecruitingApplicationGraphQlRequest(
    Long applicationFormId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    Long privacyTermId,
    Boolean privacyAgreed
) {

    public CreateRecruitingApplicationDraftCommand toMemberCommand(Long applicantMemberId) {
        return CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantMemberId(applicantMemberId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .build();
    }

    public CreateAnonymousRecruitingApplicationDraftCommand toAnonymousCommand() {
        if (privacyTermId == null || !Boolean.TRUE.equals(privacyAgreed)) {
            throw new IllegalArgumentException("비회원 지원서는 개인정보 동의가 필요합니다.");
        }
        return CreateAnonymousRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
            .privacyTermId(privacyTermId)
            .privacyAgreed(true)
            .build();
    }
}
