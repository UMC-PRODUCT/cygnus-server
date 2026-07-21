package com.umc.product.recruiting.application.port.in.command.dto;

public record CancelRecruitingRegistrationCommand(
    Long applicationId,
    Long executorMemberId
) {

    public static CancelRecruitingRegistrationCommand of(Long applicationId, Long executorMemberId) {
        return new CancelRecruitingRegistrationCommand(applicationId, executorMemberId);
    }
}
