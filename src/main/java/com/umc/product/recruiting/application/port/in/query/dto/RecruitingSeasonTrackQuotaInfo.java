package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

public record RecruitingSeasonTrackQuotaInfo(
    ChallengerTrack track,
    Integer targetCount
) {

    public static RecruitingSeasonTrackQuotaInfo from(RecruitingSeasonTrackQuota quota) {
        return new RecruitingSeasonTrackQuotaInfo(quota.getTrack(), quota.getTargetCount());
    }
}
