package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;

public record CancelRecruitingApplicationGraphQlRequest(
    String reason
) {

    public CancelRecruitingApplicationCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        return CancelRecruitingApplicationCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .reason(reason)
            .build();
    }
}
