package com.umc.product.recruiting.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;
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

@Entity
@Table(name = "recruiting_interview_evaluation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingInterviewEvaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_interview_assignment_id", nullable = false)
    private RecruitingInterviewAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Column(nullable = false, name = "evaluator_member_id")
    private Long evaluatorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingInterviewEvaluationStatus status;

    @Column
    private Integer score;

    @Column(length = 2000)
    private String comment;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingInterviewEvaluation(RecruitingInterviewAssignment assignment, Long evaluatorMemberId) {
        this.assignment = assignment;
        this.application = assignment.getApplication();
        this.evaluatorMemberId = evaluatorMemberId;
        this.status = RecruitingInterviewEvaluationStatus.DRAFT;
    }

    public static RecruitingInterviewEvaluation createDraft(
        RecruitingInterviewAssignment assignment,
        Long evaluatorMemberId
    ) {
        return RecruitingInterviewEvaluation.builder()
            .assignment(assignment)
            .evaluatorMemberId(evaluatorMemberId)
            .build();
    }

    public void updateDraft(Integer score, String comment) {
        if (this.status != RecruitingInterviewEvaluationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
        }
        this.score = score;
        this.comment = comment;
    }

    public void submit(Integer score, String comment) {
        if (this.status != RecruitingInterviewEvaluationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
        }
        this.score = score;
        this.comment = comment;
        this.status = RecruitingInterviewEvaluationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void validateNoSubmittedDuplicate(List<RecruitingInterviewEvaluation> submittedEvaluations) {
        boolean duplicated = submittedEvaluations.stream()
            .anyMatch(this::isSubmittedEvaluationForSameApplicationAndEvaluator);
        if (duplicated) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_ALREADY_SUBMITTED);
        }
    }

    private boolean isSubmittedEvaluationForSameApplicationAndEvaluator(RecruitingInterviewEvaluation other) {
        return other.status == RecruitingInterviewEvaluationStatus.SUBMITTED
            && Objects.equals(other.evaluatorMemberId, this.evaluatorMemberId)
            && isSameApplication(other.application);
    }

    private boolean isSameApplication(RecruitingApplication otherApplication) {
        if (this.application.getId() != null && otherApplication.getId() != null) {
            return Objects.equals(this.application.getId(), otherApplication.getId());
        }
        return this.application == otherApplication;
    }
}
