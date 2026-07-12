package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

public interface LoadRecruitingSeasonTrackQuotaPort {

    List<RecruitingSeasonTrackQuota> listBySeasonId(Long seasonId);

    List<RecruitingSeasonTrackQuota> listBySeasonIdForUpdate(Long seasonId);

    RecruitingSeasonTrackQuota getBySeasonIdAndTrackForUpdate(Long seasonId, ChallengerTrack track);
}
