package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Map;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

public interface LoadRecruitingSeasonTrackQuotaPort {

    List<RecruitingSeasonTrackQuota> listBySeasonId(Long seasonId);

    Map<Long, List<RecruitingSeasonTrackQuota>> listBySeasonIds(List<Long> seasonIds);

    List<RecruitingSeasonTrackQuota> listBySeasonIdForUpdate(Long seasonId);

    RecruitingSeasonTrackQuota getBySeasonIdAndTrackForUpdate(Long seasonId, ChallengerTrack track);
}
