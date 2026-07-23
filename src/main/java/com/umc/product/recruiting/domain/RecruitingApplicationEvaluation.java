package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
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
    private RecruitingApplicationEvaluationDecision decision;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    private RecruitingApplicationEvaluation(
        RecruitingApplication application,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage,
        RecruitingApplicationEvaluationDecision decision,
        String comment
    ) {
        validateIdentity(application, evaluatorMemberId, stage);
        validateDecision(decision);
        validateComment(comment);
        this.application = application;
        this.evaluatorMemberId = evaluatorMemberId;
        this.stage = stage;
        this.decision = decision;
        this.comment = comment;
        this.submittedAt = Instant.now();
    }

    public static RecruitingApplicationEvaluation create(
        RecruitingApplication application,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage,
        RecruitingApplicationEvaluationDecision decision,
        String comment
    ) {
        return new RecruitingApplicationEvaluation(application, evaluatorMemberId, stage, decision, comment);
    }

    public void revise(
        RecruitingApplicationEvaluationDecision decision,
        String comment
    ) {
        validateDecision(decision);
        validateComment(comment);
        this.decision = decision;
        this.comment = comment;
        this.submittedAt = Instant.now();
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

    private static void validateDecision(RecruitingApplicationEvaluationDecision decision) {
        if (decision == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_DECISION_REQUIRED);
        }
    }
}
