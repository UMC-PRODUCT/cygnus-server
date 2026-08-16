package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

import jakarta.persistence.LockModeType;

public interface RecruitingSeasonTrackQuotaJpaRepository extends JpaRepository<RecruitingSeasonTrackQuota, Long> {

    List<RecruitingSeasonTrackQuota> findAllBySeason_Id(Long seasonId);

    List<RecruitingSeasonTrackQuota> findAllBySeason_IdIn(List<Long> seasonIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quota from RecruitingSeasonTrackQuota quota where quota.season.id = :seasonId order by quota.id")
    List<RecruitingSeasonTrackQuota> findAllBySeasonIdForUpdate(@Param("seasonId") Long seasonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quota from RecruitingSeasonTrackQuota quota where quota.season.id = :seasonId and quota.track = :track")
    Optional<RecruitingSeasonTrackQuota> findBySeasonIdAndTrackForUpdate(
        @Param("seasonId") Long seasonId,
        @Param("track") ChallengerTrack track
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecruitingSeasonTrackQuota quota where quota.season.id = :seasonId")
    void deleteAllBySeasonId(@Param("seasonId") Long seasonId);
}
