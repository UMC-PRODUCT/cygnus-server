package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;

public record ReplaceRecruitingSeasonTrackQuotasGraphQlRequest(
    List<RecruitingSeasonTrackQuotaGraphQlRequest> quotas
) {

    public ReplaceRecruitingSeasonTrackQuotasCommand toCommand(Long seasonId) {
        return ReplaceRecruitingSeasonTrackQuotasCommand.builder()
            .seasonId(seasonId)
            .quotas(quotas.stream().map(RecruitingSeasonTrackQuotaGraphQlRequest::toCommand).toList())
            .build();
    }
}
