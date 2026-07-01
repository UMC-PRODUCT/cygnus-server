package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record CreateRecruitingApplicationDraftCommand(
    Long applicationFormId,
    Long applicantMemberId,
    String applicantIdentityKey,
    String maskedEmail
) {
}
