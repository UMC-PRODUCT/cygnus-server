package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;

public record SubmitAnonymousRecruitingApplicationGraphQlRequest(
    String email,
    String applicationKey
) {

    public SubmitAnonymousRecruitingApplicationCommand toCommand() {
        return SubmitAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail(email)
            .applicationKey(applicationKey)
            .build();
    }
}
