package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_season_track_quota",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_season_track_quota_season_track",
        columnNames = {"recruiting_season_id", "track"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingSeasonTrackQuota extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_season_id", nullable = false)
    private RecruitingSeason season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengerTrack track;

    @Column(name = "target_count", nullable = false)
    private Integer targetCount;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingSeasonTrackQuota(RecruitingSeason season, ChallengerTrack track, Integer targetCount) {
        validateTrack(track);
        validateTargetCount(targetCount);
        this.season = season;
        this.track = track;
        this.targetCount = targetCount;
    }

    public static RecruitingSeasonTrackQuota create(
        RecruitingSeason season,
        ChallengerTrack track,
        Integer targetCount
    ) {
        return RecruitingSeasonTrackQuota.builder()
            .season(season)
            .track(track)
            .targetCount(targetCount)
            .build();
    }

    public void updateTargetCount(Integer targetCount) {
        validateTargetCount(targetCount);
        this.targetCount = targetCount;
    }

    private static void validateTrack(ChallengerTrack track) {
        if (track == null || track == ChallengerTrack.INFRA_PLUS) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_UNSUPPORTED_TRACK);
        }
    }

    private static void validateTargetCount(Integer targetCount) {
        if (targetCount == null || targetCount < 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_INVALID_TARGET_COUNT);
        }
    }
}
