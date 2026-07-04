package com.umc.product.project.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;
import com.umc.product.form.domain.FormResponse;

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
 * 지원자가 제출한 지원서의 실제 내용을 담고 있는 {@link FormResponse}과 매핑해주는 엔티티입니다.
 * <p>
 * 어떤 매칭 라운드에 지원한 폼인지와 지원 결과를 담고 있습니다.
 */
@Entity
@Table(name = "project_application")
// 각 지원자는 매칭 차수 당 한 개의 활성(DRAFT/SUBMITTED) 지원서만 존재할 수 있습니다.
// JPA @UniqueConstraint는 WHERE 절을 지원하지 않아 Flyway의 partial unique index (uk_project_application_active_form_round_applicant, status IN ('DRAFT','SUBMITTED'))로 관리됩니다.
// CANCELLED 행은 인덱스 범위 밖이라 동일 (form, round, applicant) 키로 재지원이 가능합니다. (#849)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_application_form_id", nullable = false)
    private ProjectApplicationForm applicationForm; // 어떤 지원용 폼에 접수된 지원서인지

    @Column(nullable = false)
    private Long formResponseId; // 응답한 지원서 내용

    @Column(nullable = false)
    private Long applicantMemberId; // 지원자 멤버 ID

    // 참고: PostgreSQL은 null일 때 Unique 제약을 풀어주기에 중복이 없음을 보장할 수 없게 됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_matching_round_id", nullable = false)
    // 어떤 매칭 라운드에 지원한 지원서인지 나타내며, 반드시 존재하여야 합니다.
    // 추후 UMC 내에서 프로젝트 멤버를 더 구할 수 있도록 하는 경우, 별도의 엔티티를 통하여 기능을 제공하여야 할 것으로 보임.
    private ProjectMatchingRound appliedMatchingRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectApplicationStatus status;
    private Long statusChangedMemberId;
    private String statusChangeReason;

    // PM/운영진 화면의 "지원시각" 컬럼에 노출되는 시각. submit() 시 기록되며, 임시저장 단계에서는 null.
    private Instant submittedAt;

    // PM/운영진 화면의 "처리시각" 컬럼에 노출되는 시각. approve()/reject() 등 상태 전이 시 갱신되며,
    // 단순 임시저장본 갱신과 구분되어야 하므로 BaseEntity.updatedAt 과 별개 필드로 둔다.
    private Instant statusChangedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ProjectApplication(
        ProjectApplicationForm applicationForm, Long formResponseId,
        Long applicantMemberId, ProjectMatchingRound appliedMatchingRound
    ) {
        this.applicationForm = applicationForm;
        this.formResponseId = formResponseId;
        this.applicantMemberId = applicantMemberId;
        this.appliedMatchingRound = appliedMatchingRound;
        this.status = ProjectApplicationStatus.DRAFT;
    }

    public static ProjectApplication create(
        ProjectApplicationForm form, Long formResponseId,
        Long applicantMemberId, ProjectMatchingRound appliedMatchingRound
    ) {
        return ProjectApplication.builder()
            .applicationForm(form)
            .formResponseId(formResponseId)
            .applicantMemberId(applicantMemberId)
            .appliedMatchingRound(appliedMatchingRound)
            .build();
    }

    /**
     * 지원서를 합격 처리합니다.
     * <p>
     * 지원 기간이 끝난 뒤부터 결정 마감 전까지 PM 이 자유롭게 토글할 수 있으며, REJECTED 또는 APPROVED 상태에서도 재호출 가능합니다.
     * 아직 지원 기간 중이면 {@link ProjectErrorCode#PROJECT_MATCHING_ROUND_NOT_ENDED},
     * 결정 마감 후엔 {@link ProjectErrorCode#PROJECT_MATCHING_ROUND_LOCKED} 가 발생합니다.
     *
     * @param decidedByMemberId 결정한 PO 또는 운영진 ID
     * @param reason            결정 사유 (필수 아님)
     */
    public void approve(Long decidedByMemberId, String reason) {
        applyDecision(ProjectApplicationStatus.APPROVED, decidedByMemberId, reason, true);
    }

    /**
     * 지원서를 불합격 처리합니다.
     * <p>
     * 지원 기간이 끝난 뒤부터 결정 마감 전까지 PM 이 자유롭게 토글할 수 있으며, APPROVED 또는 REJECTED 상태에서도 재호출 가능합니다.
     * 아직 지원 기간 중이면 {@link ProjectErrorCode#PROJECT_MATCHING_ROUND_NOT_ENDED},
     * 결정 마감 후엔 {@link ProjectErrorCode#PROJECT_MATCHING_ROUND_LOCKED} 가 발생합니다.
     *
     * @param decidedByMemberId 결정한 PO 또는 운영진 ID
     * @param reason            결정 사유 (필수 아님)
     */
    public void reject(Long decidedByMemberId, String reason) {
        applyDecision(ProjectApplicationStatus.REJECTED, decidedByMemberId, reason, true);
    }

    /**
     * 지원서를 시간 제한과 관계없이 합격 처리합니다.
     * <p>
     * 상태 전이 규칙(SUBMITTED / APPROVED / REJECTED 만 가능)은 유지하고, 매칭 차수의 결정 가능 시간 검증만 건너뜁니다.
     *
     * @param decidedByMemberId 결정한 슈퍼어드민 ID
     * @param reason            결정 사유 (필수 아님)
     */
    public void forceApprove(Long decidedByMemberId, String reason) {
        applyDecision(ProjectApplicationStatus.APPROVED, decidedByMemberId, reason, false);
    }

    /**
     * 지원서를 시간 제한과 관계없이 불합격 처리합니다.
     * <p>
     * 상태 전이 규칙(SUBMITTED / APPROVED / REJECTED 만 가능)은 유지하고, 매칭 차수의 결정 가능 시간 검증만 건너뜁니다.
     *
     * @param decidedByMemberId 결정한 슈퍼어드민 ID
     * @param reason            결정 사유 (필수 아님)
     */
    public void forceReject(Long decidedByMemberId, String reason) {
        applyDecision(ProjectApplicationStatus.REJECTED, decidedByMemberId, reason, false);
    }

    private void applyDecision(
        ProjectApplicationStatus targetStatus, Long decidedByMemberId, String reason, boolean validateDecisionPeriod
    ) {
        validateCanBeDecided();
        if (validateDecisionPeriod) {
            appliedMatchingRound.validateIsMutableAt(Instant.now());
        }

        this.status = targetStatus;
        this.statusChangedMemberId = decidedByMemberId;
        this.statusChangeReason = reason;
        this.statusChangedAt = Instant.now();
    }

    /**
     * 차수 종료 시점의 자동 선발 결과를 반영합니다.
     * <p>
     * PM 의 단건 토글({@link #approve}/{@link #reject})과 달리 차수 잠금 검증을 거치지 않으며,
     * 정책에 따라 도출된 최종 status (APPROVED 또는 REJECTED) 를 그대로 적용합니다.
     * <p>
     * 호출자: 자동 선발 서비스 (스케줄러 / 운영진 수동 트리거 공용).
     *
     * @param targetStatus      APPROVED 또는 REJECTED 만 허용
     * @param executedByMemberId 자동 선발을 실행한 운영진 ID. 스케줄러 호출 시 {@code null}
     */
    public void applyAutoDecision(ProjectApplicationStatus targetStatus, Long executedByMemberId) {
        validateCanBeDecided();
        if (targetStatus != ProjectApplicationStatus.APPROVED
            && targetStatus != ProjectApplicationStatus.REJECTED) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }
        this.status = targetStatus;
        this.statusChangedMemberId = executedByMemberId;
        this.statusChangeReason = "auto-decide";
        this.statusChangedAt = Instant.now();
    }

    /**
     * 합/불 결정 대상이 되는 status 인지 검증합니다. SUBMITTED / APPROVED / REJECTED 만 통과합니다.
     */
    private void validateCanBeDecided() {
        if (status != ProjectApplicationStatus.SUBMITTED
            && status != ProjectApplicationStatus.APPROVED
            && status != ProjectApplicationStatus.REJECTED) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }
    }

    /**
     * 지원자가 자신의 지원서를 철회합니다 (soft delete: {@link ProjectApplicationStatus#CANCELLED} 로 상태 전이).
     * <p>
     * 정책:
     * <ul>
     *   <li>가능 상태: {@link ProjectApplicationStatus#DRAFT}, {@link ProjectApplicationStatus#SUBMITTED}</li>
     *   <li>불가 상태: {@link ProjectApplicationStatus#APPROVED} / {@link ProjectApplicationStatus#REJECTED}
     *       (이미 종결), {@link ProjectApplicationStatus#CANCELLED} (이중 취소)</li>
     * </ul>
     * 시간 제약(매칭 차수 OPEN 여부)과 행위자 권한 검증은 도메인 외부
     * ({@code ProjectApplicationCommandService} / {@code ProjectApplicationPermissionEvaluator})가 책임집니다.
     * 본 메서드는 상태 머신 전이만 보장합니다.
     *
     * @param decidedByMemberId 철회 수행자 ID (지원자 본인 또는 운영진) TODO: 운영진 철회는 아직 미구현
     * @param reason            철회 사유 (필수 아님)
     */
    public void cancel(Long decidedByMemberId, String reason) {
        switch (this.status) {
            case DRAFT, SUBMITTED -> {
                this.status = ProjectApplicationStatus.CANCELLED;
                this.statusChangedMemberId = decidedByMemberId;
                this.statusChangeReason = reason;
            }
            case APPROVED, REJECTED, CANCELLED ->
                throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_NOT_ALLOWED);
        }

        // HARD DELETE 로직
        // Draft일 떄, 즉 임시저장본 일 때 삭제하는 로직 또한 필요할 것 같음
        // -> 해당 로직이 cancel(철회) 메소드에 들어가는게 맞는지? - 삭제 메소드 별도 필요하지 않을지
    }

    /**
     * 지원서 제출 처리 (임시저장에서만 이동 가능)
     */
    public void submit() {
        if (!this.isDraft()) {
            throw new ProjectDomainException(ProjectErrorCode.APPLICATION_NOT_SUBMITTED);
        }

        this.status = ProjectApplicationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public boolean isDraft() {
        return this.status == ProjectApplicationStatus.DRAFT;
    }

    public boolean isSubmitted() {
        return this.status == ProjectApplicationStatus.SUBMITTED;
    }

    public boolean isCancelled() {
        return this.status == ProjectApplicationStatus.CANCELLED;
    }

    public void validateIsSubmitted(String message) {
        if (isSubmitted()) {
            return;
        }

        throw new ProjectDomainException(ProjectErrorCode.APPLICATION_NOT_SUBMITTED, message);
    }
}
