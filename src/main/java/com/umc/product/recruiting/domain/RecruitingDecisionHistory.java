package com.umc.product.recruiting.domain;

import java.time.Instant;
import java.util.Set;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서류/최종 판정 이력을 append-only로 보존하는 감사 기록입니다.
 * <p>
 * 판정 축과 등록 축이 {@code RecruitingApplication}의 {@code statusChanged*} 슬롯을 공유해 덮어쓰기 때문에,
 * 등록 전이 이후에도 판정 시각·담당자를 조회할 수 있도록 판정 트랜잭션에서 함께 기록합니다.
 * 담당자의 지부·학교·직위·이름·닉네임을 판정 시점 스냅샷으로 보존합니다.
 */
@Entity
@Table(name = "recruiting_decision_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingDecisionHistory extends BaseEntity {

    private static final Set<RecruitingApplicationStatus> DECISION_STATUSES = Set.of(
        RecruitingApplicationStatus.DOCUMENT_FAILED,
        RecruitingApplicationStatus.FINAL_PASSED,
        RecruitingApplicationStatus.FINAL_FAILED
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", nullable = false)
    private RecruitingApplicationStatus decisionStatus;

    @Column(name = "decided_by_member_id", nullable = false)
    private Long decidedByMemberId;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decider_role_type")
    private ChallengerRoleType deciderRoleType;

    @Column(name = "decider_chapter_id")
    private Long deciderChapterId;

    @Column(name = "decider_chapter_name")
    private String deciderChapterName;

    @Column(name = "decider_school_id")
    private Long deciderSchoolId;

    @Column(name = "decider_school_name")
    private String deciderSchoolName;

    @Column(name = "decider_name", nullable = false, length = 30)
    private String deciderName;

    @Column(name = "decider_nickname", nullable = false, length = 20)
    private String deciderNickname;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingDecisionHistory(
        RecruitingApplication application,
        RecruitingApplicationStatus decisionStatus,
        Long decidedByMemberId,
        Instant decidedAt,
        RecruitingDecisionHistoryDeciderSnapshot deciderSnapshot
    ) {
        validateRequired(application, decisionStatus, decidedByMemberId, decidedAt, deciderSnapshot);
        this.application = application;
        this.decisionStatus = decisionStatus;
        this.decidedByMemberId = decidedByMemberId;
        this.decidedAt = decidedAt;
        this.deciderRoleType = deciderSnapshot.roleType();
        this.deciderChapterId = deciderSnapshot.chapterId();
        this.deciderChapterName = deciderSnapshot.chapterName();
        this.deciderSchoolId = deciderSnapshot.schoolId();
        this.deciderSchoolName = deciderSnapshot.schoolName();
        this.deciderName = deciderSnapshot.name();
        this.deciderNickname = deciderSnapshot.nickname();
    }

    /**
     * 판정 직후의 지원서 상태와 상태 변경 시각을 그대로 기록합니다. 판정 커맨드와 같은 트랜잭션에서 호출해야 합니다.
     */
    public static RecruitingDecisionHistory create(
        RecruitingApplication application,
        RecruitingDecisionHistoryDeciderSnapshot deciderSnapshot
    ) {
        if (application == null || deciderSnapshot == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID);
        }
        return RecruitingDecisionHistory.builder()
            .application(application)
            .decisionStatus(application.getStatus())
            .decidedByMemberId(deciderSnapshot.memberId())
            .decidedAt(application.getStatusChangedAt())
            .deciderSnapshot(deciderSnapshot)
            .build();
    }

    private static void validateRequired(
        RecruitingApplication application,
        RecruitingApplicationStatus decisionStatus,
        Long decidedByMemberId,
        Instant decidedAt,
        RecruitingDecisionHistoryDeciderSnapshot deciderSnapshot
    ) {
        if (application == null
            || decisionStatus == null
            || !DECISION_STATUSES.contains(decisionStatus)
            || decidedByMemberId == null
            || decidedAt == null
            || deciderSnapshot == null
            || deciderSnapshot.name() == null
            || deciderSnapshot.nickname() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID);
        }
    }
}
