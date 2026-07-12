package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
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
    name = "recruiting_application_evaluation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_application_evaluation_application_evaluator_stage",
        columnNames = {"recruiting_application_id", "evaluator_member_id", "stage"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplicationEvaluation extends BaseEntity {

    private static final int MAX_COMMENT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Column(name = "evaluator_member_id", nullable = false)
    private Long evaluatorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingEvaluatorStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingApplicationEvaluationStatus status;

    @Enumerated(EnumType.STRING)
    private RecruitingApplicationEvaluationDecision decision;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingApplicationEvaluation(
        RecruitingApplication application,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    ) {
        validateIdentity(application, evaluatorMemberId, stage);
        this.application = application;
        this.evaluatorMemberId = evaluatorMemberId;
        this.stage = stage;
        this.status = RecruitingApplicationEvaluationStatus.DRAFT;
    }

    public static RecruitingApplicationEvaluation createDraft(
        RecruitingApplication application,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    ) {
        return RecruitingApplicationEvaluation.builder()
            .application(application)
            .evaluatorMemberId(evaluatorMemberId)
            .stage(stage)
            .build();
    }

    public void updateDraft(RecruitingApplicationEvaluationDecision decision, String comment) {
        requireDraft();
        validateComment(comment);
        this.decision = decision;
        this.comment = comment;
    }

    public void submit(RecruitingApplicationEvaluationDecision decision, String comment) {
        requireDraft();
        if (decision == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_DECISION_REQUIRED);
        }
        validateComment(comment);
        this.decision = decision;
        this.comment = comment;
        this.status = RecruitingApplicationEvaluationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public boolean isSubmitted() {
        return status == RecruitingApplicationEvaluationStatus.SUBMITTED;
    }

    private static void validateIdentity(
        RecruitingApplication application,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    ) {
        if (application == null || evaluatorMemberId == null || evaluatorMemberId <= 0 || stage == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID);
        }
    }

    private static void validateComment(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_COMMENT_TOO_LONG);
        }
    }

    private void requireDraft() {
        if (status != RecruitingApplicationEvaluationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
        }
    }
}
