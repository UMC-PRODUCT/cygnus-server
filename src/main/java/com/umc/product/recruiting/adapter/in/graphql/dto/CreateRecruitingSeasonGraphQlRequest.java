package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

public record CreateRecruitingSeasonGraphQlRequest(
    String gisuId,
    String schoolId,
    List<RecruitingSeasonTrackQuotaGraphQlRequest> quotas
) {

    public CreateRecruitingSeasonCommand toCommand(Long requesterMemberId) {
        return CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(requesterMemberId)
            .gisuId(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU))
            .schoolId(GlobalId.decodeLong(schoolId, GlobalIdTypes.SCHOOL))
            .quotas(quotas == null
                ? List.of()
                : quotas.stream().map(RecruitingSeasonTrackQuotaGraphQlRequest::toCommand).toList())
            .build();
    }
}
