package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingEvaluatorQuestionDomainTest {

    @Test
    @DisplayName("평가자는 모집 차수와 회원 및 평가 단계별로 생성된다")
    void createRoundEvaluatorByStage() {
        RecruitingRound round = mock(RecruitingRound.class);

        RecruitingRoundEvaluator evaluator = RecruitingRoundEvaluator.create(
            round,
            10L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        assertThat(evaluator.getRound()).isSameAs(round);
        assertThat(evaluator.getMemberId()).isEqualTo(10L);
        assertThat(evaluator.getStage()).isEqualTo(RecruitingEvaluatorStage.DOCUMENT);
    }

    @Test
    @DisplayName("평가자 회원 식별자는 양수여야 한다")
    void rejectInvalidEvaluatorMemberId() {
        RecruitingRound round = mock(RecruitingRound.class);

        assertThatThrownBy(() -> RecruitingRoundEvaluator.create(round, 0L, RecruitingEvaluatorStage.INTERVIEW))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_INVALID);
    }

    @Test
    @DisplayName("평가자 단계는 필수다")
    void rejectMissingEvaluatorStage() {
        RecruitingRound round = mock(RecruitingRound.class);

        assertThatThrownBy(() -> RecruitingRoundEvaluator.create(round, 10L, null))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_INVALID);
    }

    @Test
    @DisplayName("공통 면접 질문을 수정한다")
    void updateRoundInterviewQuestion() {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(round, "공통 질문", 0);

        question.updateBeforeFirstEvaluationSubmission("수정 질문", 1);

        assertThat(question.getContent()).isEqualTo("수정 질문");
        assertThat(question.getOrderNo()).isEqualTo(1);
        assertThat(question.isActive()).isTrue();
    }

    @Test
    @DisplayName("공통 면접 질문을 비활성화한다")
    void deactivateRoundInterviewQuestion() {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(round, "공통 질문", 0);

        question.deactivateBeforeFirstEvaluationSubmission();

        assertThat(question.isActive()).isFalse();
    }

    @Test
    @DisplayName("개별 면접 질문을 수정한다")
    void updateApplicationInterviewQuestion() {
        RecruitingApplication application = mock(RecruitingApplication.class);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "개별 질문",
            2
        );

        question.updateBeforeFirstEvaluationSubmission("수정 개별 질문", 3);

        assertThat(question.getApplication()).isSameAs(application);
        assertThat(question.getContent()).isEqualTo("수정 개별 질문");
        assertThat(question.getOrderNo()).isEqualTo(3);
        assertThat(question.isActive()).isTrue();
    }

    @Test
    @DisplayName("개별 면접 질문을 비활성화한다")
    void deactivateApplicationInterviewQuestion() {
        RecruitingApplication application = mock(RecruitingApplication.class);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "개별 질문",
            2
        );

        question.deactivateBeforeFirstEvaluationSubmission();

        assertThat(question.isActive()).isFalse();
    }

    @Test
    @DisplayName("면접 질문 내용은 공백일 수 없다")
    void rejectBlankInterviewQuestionContent() {
        RecruitingRound round = mock(RecruitingRound.class);

        assertThatThrownBy(() -> RecruitingRoundInterviewQuestion.create(round, "  ", 0))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_CONTENT);
    }

    @Test
    @DisplayName("면접 질문 순서는 0 이상이어야 한다")
    void rejectNegativeInterviewQuestionOrder() {
        RecruitingApplication application = mock(RecruitingApplication.class);

        assertThatThrownBy(() -> RecruitingApplicationInterviewQuestion.create(application, "질문", -1))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_ORDER_NO);
    }
}
