package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

public record RecruitingSeasonTrackQuotaCommand(
    ChallengerTrack track,
    Integer targetCount
) {

    public static RecruitingSeasonTrackQuotaCommand of(ChallengerTrack track, Integer targetCount) {
        return new RecruitingSeasonTrackQuotaCommand(track, targetCount);
    }
}
