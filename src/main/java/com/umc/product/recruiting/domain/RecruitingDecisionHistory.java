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
 * 담당자의 직위는 판정 이후 역할 변경과 무관하게 당시 상태를 스냅샷으로 보존하며,
 * 이름·닉네임은 조회 시점에 member 도메인에서, 소속 학교는 직위와 지원서의 학교로 유도해 결합합니다.
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

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingDecisionHistory(
        RecruitingApplication application,
        RecruitingApplicationStatus decisionStatus,
        Long decidedByMemberId,
        Instant decidedAt,
        ChallengerRoleType deciderRoleType
    ) {
        validateRequired(application, decisionStatus, decidedByMemberId, decidedAt);
        this.application = application;
        this.decisionStatus = decisionStatus;
        this.decidedByMemberId = decidedByMemberId;
        this.decidedAt = decidedAt;
        this.deciderRoleType = deciderRoleType;
    }

    /**
     * 판정 직후의 지원서 상태와 상태 변경 시각을 그대로 기록합니다. 판정 커맨드와 같은 트랜잭션에서 호출해야 합니다.
     */
    public static RecruitingDecisionHistory create(
        RecruitingApplication application,
        Long decidedByMemberId,
        ChallengerRoleType deciderRoleType
    ) {
        if (application == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID);
        }
        return RecruitingDecisionHistory.builder()
            .application(application)
            .decisionStatus(application.getStatus())
            .decidedByMemberId(decidedByMemberId)
            .decidedAt(application.getStatusChangedAt())
            .deciderRoleType(deciderRoleType)
            .build();
    }

    private static void validateRequired(
        RecruitingApplication application,
        RecruitingApplicationStatus decisionStatus,
        Long decidedByMemberId,
        Instant decidedAt
    ) {
        if (application == null
            || decisionStatus == null
            || !DECISION_STATUSES.contains(decisionStatus)
            || decidedByMemberId == null
            || decidedAt == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID);
        }
    }
}
