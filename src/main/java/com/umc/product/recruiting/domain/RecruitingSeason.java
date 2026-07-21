package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingSeason(Long gisuId, Long schoolId, String memo) {
        this.gisuId = gisuId;
        this.schoolId = schoolId;
        this.memo = normalizeMemo(memo);
    }

    public static RecruitingSeason create(Long gisuId, Long schoolId) {
        return create(gisuId, schoolId, null);
    }

    public static RecruitingSeason create(Long gisuId, Long schoolId, String memo) {
        return RecruitingSeason.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .memo(memo)
            .build();
    }

    public void updateMemo(String memo) {
        this.memo = normalizeMemo(memo);
    }

    private static String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        return memo.trim();
    }
}
