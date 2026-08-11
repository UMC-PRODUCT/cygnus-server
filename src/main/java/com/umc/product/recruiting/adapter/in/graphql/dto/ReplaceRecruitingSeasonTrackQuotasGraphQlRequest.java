package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;

public record ReplaceRecruitingSeasonTrackQuotasGraphQlRequest(
    String seasonId,
    List<RecruitingSeasonTrackQuotaGraphQlRequest> quotas
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public ReplaceRecruitingSeasonTrackQuotasCommand toCommand() {
        return ReplaceRecruitingSeasonTrackQuotasCommand.builder()
            .seasonId(decodedSeasonId())
            .quotas(quotas.stream().map(RecruitingSeasonTrackQuotaGraphQlRequest::toCommand).toList())
            .build();
    }
}
