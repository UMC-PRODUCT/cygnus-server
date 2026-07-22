package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;

public record UpdateRecruitingSeasonGraphQlRequest(
    String memo,
    List<RecruitingSeasonTrackQuotaGraphQlRequest> quotas
) {

    public UpdateRecruitingSeasonCommand toCommand(Long seasonId) {
        return UpdateRecruitingSeasonCommand.builder()
            .seasonId(seasonId)
            .memo(memo)
            .build();
    }

    public ReplaceRecruitingSeasonTrackQuotasGraphQlRequest quotaReplacement() {
        return new ReplaceRecruitingSeasonTrackQuotasGraphQlRequest(quotas == null ? List.of() : quotas);
    }
}
