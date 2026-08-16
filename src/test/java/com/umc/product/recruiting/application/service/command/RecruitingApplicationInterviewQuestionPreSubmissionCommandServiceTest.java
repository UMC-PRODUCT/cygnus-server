package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationInterviewQuestionPreSubmissionCommandServiceTest {

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
        sut = new RecruitingApplicationInterviewQuestionCommandService(
            loadQuestionPort,
            saveQuestionPort,
            loadEvaluatorPort,
            new RecruitingInterviewQuestionMutationPolicy(loadSubmittedEvaluationPort),
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 차수 평가자가 개별 질문을 생성할 수 있다")
    void createBeforeFirstSubmission() {
        RecruitingApplication application = whitelistedApplication();
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(false);
        given(saveQuestionPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        sut.createApplicationQuestion(CreateRecruitingApplicationInterviewQuestionCommand.of(
            2L,
            99L,
            "새 질문",
            0
        ));

        then(saveQuestionPort).should().save(argThat(question -> question.getApplication() == application
            && question.getContent().equals("새 질문")
            && question.getOrderNo() == 0
            && question.isActive()));
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 차수 평가자가 개별 질문을 비활성화할 수 있다")
    void deactivateBeforeFirstSubmission() {
        RecruitingApplication application = whitelistedApplication();
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            "기존 질문",
            0
        );
        given(loadQuestionPort.getById(201L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByApplicationId(2L)).willReturn(false);

        sut.deactivateApplicationQuestion(
            DeactivateRecruitingApplicationInterviewQuestionCommand.of(201L, 2L, 99L)
        );

        assertThat(question.isActive()).isFalse();
        then(saveQuestionPort).should().save(question);
    }

    private RecruitingApplication whitelistedApplication() {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        RecruitingApplication application = mock(RecruitingApplication.class);
        given(application.getId()).willReturn(2L);
        given(application.getRound()).willReturn(round);
        given(concurrencyLockService.lockRoundThenApplication(2L)).willReturn(application);
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 99L)).willReturn(true);
        return application;
    }
}
