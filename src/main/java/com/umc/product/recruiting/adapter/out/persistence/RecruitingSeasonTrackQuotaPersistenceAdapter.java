package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingSeasonTrackQuotaPersistenceAdapter implements
    LoadRecruitingSeasonTrackQuotaPort,
    SaveRecruitingSeasonTrackQuotaPort {

    private final RecruitingSeasonTrackQuotaJpaRepository repository;

    @Override
    public List<RecruitingSeasonTrackQuota> listBySeasonId(Long seasonId) {
        return repository.findAllBySeason_Id(seasonId).stream()
            .sorted(Comparator.comparingInt(quota -> quota.getTrack().getSortOrder()))
            .toList();
    }

    @Override
    public Map<Long, List<RecruitingSeasonTrackQuota>> listBySeasonIds(List<Long> seasonIds) {
        if (seasonIds.isEmpty()) {
            return Map.of();
        }
        return repository.findAllBySeason_IdIn(seasonIds).stream()
            .collect(Collectors.groupingBy(quota -> quota.getSeason().getId()));
    }

    @Override
    public List<RecruitingSeasonTrackQuota> listBySeasonIdForUpdate(Long seasonId) {
        return repository.findAllBySeasonIdForUpdate(seasonId);
    }

    @Override
    public RecruitingSeasonTrackQuota getBySeasonIdAndTrackForUpdate(Long seasonId, ChallengerTrack track) {
        return repository.findBySeasonIdAndTrackForUpdate(seasonId, track)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_NOT_FOUND));
    }

    @Override
    public List<RecruitingSeasonTrackQuota> saveAll(List<RecruitingSeasonTrackQuota> quotas) {
        return repository.saveAll(quotas);
    }

    @Override
    public void deleteAll(List<RecruitingSeasonTrackQuota> quotas) {
        repository.deleteAll(quotas);
    }

    @Override
    public void deleteAllBySeasonId(Long seasonId) {
        repository.deleteAllBySeasonId(seasonId);
    }
}
