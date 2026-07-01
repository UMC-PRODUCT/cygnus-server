package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRecruitingApplicationDraftRequest(
    @NotNull Long applicationFormId,
    Long applicantMemberId,
    @NotBlank String applicantIdentityKey,
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
