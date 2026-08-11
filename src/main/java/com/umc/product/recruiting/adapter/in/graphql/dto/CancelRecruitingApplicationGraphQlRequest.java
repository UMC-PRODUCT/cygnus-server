package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;

public record CancelRecruitingApplicationGraphQlRequest(
    String applicationId,
    String reason
) {

    public CancelRecruitingApplicationCommand toCommand(Long resolvedRequesterMemberId) {
        return CancelRecruitingApplicationCommand.builder()
            .applicationId(GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION))
            .requesterMemberId(resolvedRequesterMemberId)
            .reason(reason)
            .build();
    }
}
