package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;

public record SubmitRecruitingApplicationGraphQlRequest(
    String applicationId,
    String submittedIp
) {

    public SubmitRecruitingApplicationCommand toCommand(Long resolvedRequesterMemberId) {
        return SubmitRecruitingApplicationCommand.builder()
            .applicationId(GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION))
            .requesterMemberId(resolvedRequesterMemberId)
            .submittedIp(submittedIp)
            .build();
    }
}
