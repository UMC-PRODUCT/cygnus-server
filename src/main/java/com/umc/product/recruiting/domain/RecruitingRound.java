package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
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
    name = "recruiting_round",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_round_season_type_no",
        columnNames = {"recruiting_season_id", "type", "round_no"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingRound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_season_id", nullable = false)
    private RecruitingSeason season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingRoundType type;

    @Column(nullable = false, name = "round_no")
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingRoundStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingRound(RecruitingSeason season, RecruitingRoundType type, Integer roundNo) {
        validateRoundNo(roundNo);
        this.season = season;
        this.type = type;
        this.roundNo = roundNo;
        this.status = RecruitingRoundStatus.DRAFT;
    }

    public static RecruitingRound createRegular(RecruitingSeason season) {
        return RecruitingRound.builder()
            .season(season)
            .type(RecruitingRoundType.REGULAR)
            .roundNo(1)
            .build();
    }

    public static RecruitingRound createAdditional(RecruitingSeason season, Integer roundNo) {
        return RecruitingRound.builder()
            .season(season)
            .type(RecruitingRoundType.ADDITIONAL)
            .roundNo(roundNo)
            .build();
    }

    private static void validateRoundNo(Integer roundNo) {
        if (roundNo == null || roundNo < 1) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_ROUND_NO);
        }
    }
}
