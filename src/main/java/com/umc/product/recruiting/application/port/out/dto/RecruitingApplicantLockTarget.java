package com.umc.product.recruiting.application.port.out.dto;

public record RecruitingApplicantLockTarget(
    Long gisuId,
    Long applicantMemberId,
    String applicantEmail
) {
}
