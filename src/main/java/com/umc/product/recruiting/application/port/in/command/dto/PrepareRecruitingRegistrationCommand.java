package com.umc.product.recruiting.application.port.in.command.dto;

public record PrepareRecruitingRegistrationCommand(
    Long applicationId,
    Long executorMemberId
) {

    public static PrepareRecruitingRegistrationCommand of(Long applicationId, Long executorMemberId) {
        return new PrepareRecruitingRegistrationCommand(applicationId, executorMemberId);
    }
}
