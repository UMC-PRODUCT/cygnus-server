package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingSeasonTrackQuotaCommand;

public record RecruitingSeasonTrackQuotaGraphQlRequest(
    ChallengerTrack track,
    Integer targetCount
) {

    public RecruitingSeasonTrackQuotaCommand toCommand() {
        return RecruitingSeasonTrackQuotaCommand.of(track, targetCount);
    }
}
