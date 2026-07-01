package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;

public record SubmitRecruitingApplicationRequest(
    Long requesterMemberId,
    String submittedIp
) {

    public SubmitRecruitingApplicationCommand toCommand(
        Long applicationId,
        Long resolvedRequesterMemberId,
        String fallbackSubmittedIp
    ) {
        return SubmitRecruitingApplicationCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .submittedIp(submittedIp == null ? fallbackSubmittedIp : submittedIp)
            .build();
    }
}
