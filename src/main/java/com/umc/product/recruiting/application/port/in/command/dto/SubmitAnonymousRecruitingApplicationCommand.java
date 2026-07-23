package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record SubmitAnonymousRecruitingApplicationCommand(
    String credentialEmail,
    String applicationKey,
    String submittedIp
) {
}
