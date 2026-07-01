package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_season",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_season_gisu_school",
        columnNames = {"gisu_id", "school_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingSeason extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "gisu_id")
    private Long gisuId;

    @Column(nullable = false, name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingSeasonStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingSeason(Long gisuId, Long schoolId) {
        this.gisuId = gisuId;
        this.schoolId = schoolId;
        this.status = RecruitingSeasonStatus.DRAFT;
    }

    public static RecruitingSeason create(Long gisuId, Long schoolId) {
        return RecruitingSeason.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .build();
    }
}
