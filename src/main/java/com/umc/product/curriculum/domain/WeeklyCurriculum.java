package com.umc.product.curriculum.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "weekly_curriculum",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_weekly_curriculum_curriculum_id_week_no_extra",
            columnNames = {"curriculum_id", "week_no", "is_extra"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class WeeklyCurriculum extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @Column(name = "is_extra", nullable = false)
    private boolean isExtra;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "week_no", nullable = false)
    private Long weekNo;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Builder(access = AccessLevel.PRIVATE)
    private WeeklyCurriculum(
        Curriculum curriculum, boolean isExtra,
        String title, Long weekNo,
        Instant startsAt, Instant endsAt
    ) {
        this.curriculum = curriculum;
        this.isExtra = isExtra;
        this.title = title;
        this.weekNo = weekNo;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        validateStartBeforeEnd();
    }

    public static WeeklyCurriculum create(
        Curriculum curriculum, Long weekNo, boolean isExtra, String title,
        Instant startsAt, Instant endsAt
    ) {
        return WeeklyCurriculum.builder()
            .curriculum(curriculum)
            .weekNo(weekNo)
            .isExtra(isExtra)
            .title(title)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build();
    }

    /**
     * 주차별 커리큘럼 전체 수정. null 필드는 기존 값을 유지한다. startsAt/endsAt 변경 시 start ≤ end 검증.
     */
    public void update(Long weekNo, Boolean isExtra, String title, Instant startsAt, Instant endsAt) {
        updateWeekNo(weekNo);
        updateIsExtra(isExtra);
        updateTitle(title);
        updatePeriod(startsAt, endsAt);
    }

    public void updateTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }

    public void updateWeekNo(Long weekNo) {
        if (weekNo != null) {
            this.weekNo = weekNo;
        }
    }

    public void updateIsExtra(Boolean isExtra) {
        if (isExtra != null) {
            this.isExtra = isExtra;
        }
    }

    public void updatePeriod(Instant startsAt, Instant endsAt) {
        Instant newStartsAt = startsAt != null ? startsAt : this.startsAt;
        Instant newEndsAt = endsAt != null ? endsAt : this.endsAt;
        if (newStartsAt.isAfter(newEndsAt)) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_WEEKLY_CURRICULUM_PERIOD);
        }
        this.startsAt = newStartsAt;
        this.endsAt = newEndsAt;
    }

    private void validateStartBeforeEnd() {
        if (startsAt.isAfter(endsAt)) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_WEEKLY_CURRICULUM_PERIOD);
        }
    }
}
