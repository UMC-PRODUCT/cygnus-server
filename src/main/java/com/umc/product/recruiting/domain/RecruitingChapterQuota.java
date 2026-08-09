package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

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
    name = "recruiting_chapter_quota",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_chapter_quota_gisu_chapter",
        columnNames = {"gisu_id", "chapter_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingChapterQuota extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "gisu_id")
    private Long gisuId;

    @Column(nullable = false, name = "chapter_id")
    private Long chapterId;

    @Column(nullable = false, name = "total_target_count")
    private Integer totalTargetCount;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingChapterQuota(Long gisuId, Long chapterId, Integer totalTargetCount) {
        validateTotalTargetCount(totalTargetCount);
        this.gisuId = gisuId;
        this.chapterId = chapterId;
        this.totalTargetCount = totalTargetCount;
    }

    public static RecruitingChapterQuota create(Long gisuId, Long chapterId, Integer totalTargetCount) {
        return RecruitingChapterQuota.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .totalTargetCount(totalTargetCount)
            .build();
    }

    public void updateTotalTargetCount(Integer totalTargetCount) {
        validateTotalTargetCount(totalTargetCount);
        this.totalTargetCount = totalTargetCount;
    }

    private static void validateTotalTargetCount(Integer totalTargetCount) {
        if (totalTargetCount == null || totalTargetCount < 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_CHAPTER_QUOTA_INVALID_TARGET_COUNT);
        }
    }
}
