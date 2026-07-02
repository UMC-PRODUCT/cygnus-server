package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;

public record CreateRecruitingApplicationDraftGraphQlRequest(
    Long applicationFormId,
    Long applicantMemberId,
    String applicantIdentityKey,
    String maskedEmail
) {

    public CreateRecruitingApplicationDraftCommand toCommand(Long resolvedApplicantMemberId) {
        return CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(applicationFormId)
            .applicantMemberId(resolvedApplicantMemberId)
            .applicantIdentityKey(applicantIdentityKey)
            .maskedEmail(maskedEmail)
            .build();
    }
}
