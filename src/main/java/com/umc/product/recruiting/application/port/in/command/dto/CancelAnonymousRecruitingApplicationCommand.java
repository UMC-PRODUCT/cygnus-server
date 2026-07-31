package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record CancelAnonymousRecruitingApplicationCommand(
    String credentialEmail,
    String applicationKey
) {
}
