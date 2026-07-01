package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;

public record CancelRecruitingApplicationRequest(
    Long requesterMemberId,
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
