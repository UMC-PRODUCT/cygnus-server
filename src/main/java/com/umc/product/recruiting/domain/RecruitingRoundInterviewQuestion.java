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
@Table(name = "recruiting_round_interview_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingRoundInterviewQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_round_id", nullable = false)
    private RecruitingRound round;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(nullable = false)
    private boolean active;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingRoundInterviewQuestion(RecruitingRound round, String content, Integer orderNo) {
        validateTarget(round);
        validateContent(content);
        validateOrderNo(orderNo);
        this.round = round;
        this.content = content;
        this.orderNo = orderNo;
        this.active = true;
    }

    public static RecruitingRoundInterviewQuestion create(
        RecruitingRound round,
        String content,
        Integer orderNo
    ) {
        return RecruitingRoundInterviewQuestion.builder()
            .round(round)
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

    private static void validateTarget(RecruitingRound round) {
        if (round == null) {
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
