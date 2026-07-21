package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record ConfirmRecruitingRegistrationCommand(
    Long applicationId,
    Long executorMemberId
) {
}
