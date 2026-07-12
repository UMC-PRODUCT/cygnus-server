package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReplaceRecruitingSeasonTrackQuotasCommand(
    Long seasonId,
    List<RecruitingSeasonTrackQuotaCommand> quotas
) {

    public ReplaceRecruitingSeasonTrackQuotasCommand {
        quotas = quotas == null ? List.of() : List.copyOf(quotas);
    }
}
