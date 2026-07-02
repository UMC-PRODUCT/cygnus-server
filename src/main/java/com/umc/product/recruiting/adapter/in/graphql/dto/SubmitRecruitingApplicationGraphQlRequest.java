package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;

public record SubmitRecruitingApplicationGraphQlRequest(
    Long requesterMemberId,
    String submittedIp
) {

    public SubmitRecruitingApplicationCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        return SubmitRecruitingApplicationCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .submittedIp(submittedIp)
            .build();
    }
}
