package com.umc.product.curriculum.domain;

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
    name = "weekly_best_workbook",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_weekly_best_workbook_study_group_week",
            columnNames = {"study_group_id", "weekly_curriculum_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyBestWorkbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "study_group_id", nullable = false)
    private Long studyGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_curriculum_id", nullable = false)
    private WeeklyCurriculum weeklyCurriculum;

    @Column(name = "best_reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private Long decidedMemberId;

    // 베스트 워크북 선정 시 상점 부여는 Service 단에서 자동으로 묶어둘것 !

    @Builder(access = AccessLevel.PRIVATE)
    private WeeklyBestWorkbook(
        Long memberId,
        Long studyGroupId,
        WeeklyCurriculum weeklyCurriculum,
        String reason,
        Long decidedMemberId
    ) {
        if (memberId == null || studyGroupId == null || weeklyCurriculum == null || decidedMemberId == null) {
            throw new IllegalArgumentException("베스트 워크북 필수 값이 누락되었습니다.");
        }
        validateReason(reason);
        this.memberId = memberId;
        this.studyGroupId = studyGroupId;
        this.weeklyCurriculum = weeklyCurriculum;
        this.reason = reason;
        this.decidedMemberId = decidedMemberId;
    }

    public static WeeklyBestWorkbook create(
        WeeklyCurriculum weeklyCurriculum,
        Long memberId,
        Long studyGroupId,
        String reason,
        Long decidedMemberId
    ) {
        return WeeklyBestWorkbook.builder()
            .weeklyCurriculum(weeklyCurriculum)
            .memberId(memberId)
            .studyGroupId(studyGroupId)
            .reason(reason)
            .decidedMemberId(decidedMemberId)
            .build();
    }

    public void editReason(String reason) {
        validateReason(reason);
        this.reason = reason;
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REASON_REQUIRED);
        }
    }
}
