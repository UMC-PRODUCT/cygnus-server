package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

public interface SaveRecruitingSeasonTrackQuotaPort {

    List<RecruitingSeasonTrackQuota> saveAll(List<RecruitingSeasonTrackQuota> quotas);

    void deleteAll(List<RecruitingSeasonTrackQuota> quotas);

    void deleteAllBySeasonId(Long seasonId);
}
