package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationInterviewQuestionCommandServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    LoadRecruitingApplicationInterviewQuestionPort loadQuestionPort;

    @Mock
    SaveRecruitingApplicationInterviewQuestionPort saveQuestionPort;

    @Mock
    LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;

    @Mock
    LoadRecruitingSubmittedInterviewEvaluationPort loadSubmittedEvaluationPort;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingApplicationInterviewQuestionCommandService sut;

    @BeforeEach
    void setUp() {
        RecruitingInterviewQuestionMutationPolicy mutationPolicy =
            new RecruitingInterviewQuestionMutationPolicy(loadSubmittedEvaluationPort);
        sut = new RecruitingApplicationInterviewQuestionCommandService(
            loadQuestionPort,
            saveQuestionPort,
            loadEvaluatorPort,
            mutationPolicy,
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 INTERVIEW 평가자가 개별 질문을 수정할 수 있다")
    void updateBeforeFirstSubmission() {
        RecruitingApplication application = whitelistedApplication(2L, 1L, 99L);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "기존 질문",
            0
        );
        given(loadQuestionPort.getById(201L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(false);

        sut.updateApplicationQuestion(UpdateRecruitingApplicationInterviewQuestionCommand.of(
            201L,
            2L,
            99L,
            "수정 질문",
            1
        ));

        assertThat(question.getContent()).isEqualTo("수정 질문");
        assertThat(question.getOrderNo()).isEqualTo(1);
        then(saveQuestionPort).should().save(question);
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 개별 질문 생성을 거부한다")
    void rejectCreationAfterFirstSubmission() {
        RecruitingApplication application = whitelistedApplication(2L, 1L, 99L);
        given(concurrencyLockService.lockRoundThenApplication(2L)).willReturn(application);
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(true);

        assertThatThrownBy(() -> sut.createApplicationQuestion(
            CreateRecruitingApplicationInterviewQuestionCommand.of(2L, 99L, "새 질문", 0)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        then(saveQuestionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 개별 질문 수정을 거부한다")
    void rejectUpdateAfterFirstSubmission() {
        RecruitingApplication application = whitelistedApplication(2L, 1L, 99L);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "기존 질문",
            0
        );
        given(loadQuestionPort.getById(201L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(true);

        assertThatThrownBy(() -> sut.updateApplicationQuestion(
            UpdateRecruitingApplicationInterviewQuestionCommand.of(201L, 2L, 99L, "수정 질문", 1)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(question.getContent()).isEqualTo("기존 질문");
        then(saveQuestionPort).should(never()).save(question);
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 개별 질문 비활성화를 거부한다")
    void rejectDeactivationAfterFirstSubmission() {
        RecruitingApplication application = whitelistedApplication(2L, 1L, 99L);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "기존 질문",
            0
        );
        given(loadQuestionPort.getById(201L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(true);

        assertThatThrownBy(() -> sut.deactivateApplicationQuestion(
            DeactivateRecruitingApplicationInterviewQuestionCommand.of(201L, 2L, 99L)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(question.isActive()).isTrue();
        then(saveQuestionPort).should(never()).save(question);
    }

    @Test
    @DisplayName("DOCUMENT 전용 평가자는 개별 질문을 수정할 수 없다")
    void rejectUpdateForDocumentOnlyEvaluator() {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        RecruitingApplication application = mock(RecruitingApplication.class);
        given(application.getRound()).willReturn(round);
        given(concurrencyLockService.lockRoundThenApplication(2L)).willReturn(application);
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            eq(1L),
            eq(99L),
            any(RecruitingEvaluatorStage.class)
        )).willAnswer(invocation -> invocation.getArgument(2) == RecruitingEvaluatorStage.DOCUMENT);

        assertThatThrownBy(() -> sut.updateApplicationQuestion(
            UpdateRecruitingApplicationInterviewQuestionCommand.of(201L, 2L, 99L, "수정 질문", 1)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        then(loadEvaluatorPort).should().existsByRoundIdAndMemberIdAndStage(
            1L,
            99L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        then(loadSubmittedEvaluationPort).shouldHaveNoInteractions();
        then(saveQuestionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 지원서와 질문 지원서가 다르면 개별 질문 수정을 거부한다")
    void rejectUpdateForDifferentApplicationScope() {
        RecruitingRound requestedRound = mock(RecruitingRound.class);
        given(requestedRound.getId()).willReturn(1L);
        RecruitingApplication requestedApplication = mock(RecruitingApplication.class);
        given(requestedApplication.getRound()).willReturn(requestedRound);
        given(concurrencyLockService.lockRoundThenApplication(2L)).willReturn(requestedApplication);
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            1L,
            99L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(true);
        RecruitingApplication application = mock(RecruitingApplication.class);
        given(application.getId()).willReturn(3L);
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "기존 질문",
            0
        );
        given(loadQuestionPort.getById(201L)).willReturn(question);

        assertThatThrownBy(() -> sut.updateApplicationQuestion(
            UpdateRecruitingApplicationInterviewQuestionCommand.of(201L, 2L, 99L, "수정 질문", 1)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        then(loadSubmittedEvaluationPort).shouldHaveNoInteractions();
    }

    private RecruitingApplication whitelistedApplication(Long applicationId, Long roundId, Long requesterMemberId) {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(roundId);
        RecruitingApplication application = mock(RecruitingApplication.class);
        given(application.getId()).willReturn(applicationId);
        given(application.getRound()).willReturn(round);
        given(concurrencyLockService.lockRoundThenApplication(applicationId)).willReturn(application);
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            roundId,
            requesterMemberId,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(true);
        return application;
    }
}
