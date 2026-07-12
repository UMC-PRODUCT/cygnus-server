package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "recruiting_application_interview_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplicationInterviewQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(nullable = false)
    private boolean active;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingApplicationInterviewQuestion(
        RecruitingApplication application,
        String content,
        Integer orderNo
    ) {
        validateTarget(application);
        validateContent(content);
        validateOrderNo(orderNo);
        this.application = application;
        this.content = content;
        this.orderNo = orderNo;
        this.active = true;
    }

    public static RecruitingApplicationInterviewQuestion create(
        RecruitingApplication application,
        String content,
        Integer orderNo
    ) {
        return RecruitingApplicationInterviewQuestion.builder()
            .application(application)
            .content(content)
            .orderNo(orderNo)
            .build();
    }

    public void assertEditableBeforeFirstEvaluationSubmission(boolean hasSubmittedEvaluation) {
        if (hasSubmittedEvaluation) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        }
    }

    public void updateBeforeFirstEvaluationSubmission(String content, Integer orderNo) {
        validateContent(content);
        validateOrderNo(orderNo);
        this.content = content;
        this.orderNo = orderNo;
    }

    public void deactivateBeforeFirstEvaluationSubmission() {
        this.active = false;
    }

    private static void validateTarget(RecruitingApplication application) {
        if (application == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_TARGET);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_CONTENT);
        }
    }

    private static void validateOrderNo(Integer orderNo) {
        if (orderNo == null || orderNo < 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_ORDER_NO);
        }
    }
}
