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

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundInterviewQuestionPreSubmissionCommandServiceTest {

    @Mock
    LoadRecruitingRoundInterviewQuestionPort loadQuestionPort;

    @Mock
    SaveRecruitingRoundInterviewQuestionPort saveQuestionPort;

    @Mock
    LoadRecruitingSubmittedInterviewEvaluationPort loadSubmittedEvaluationPort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingRoundInterviewQuestionCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingRoundInterviewQuestionCommandService(
            loadQuestionPort,
            saveQuestionPort,
            authorizeManagementUseCase,
            new RecruitingInterviewQuestionMutationPolicy(loadSubmittedEvaluationPort),
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 공통 질문을 생성할 수 있다")
    void createBeforeFirstSubmission() {
        RecruitingRound round = authorizedRound();
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(false);
        given(saveQuestionPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        sut.createRoundQuestion(CreateRecruitingRoundInterviewQuestionCommand.of(1L, 99L, "새 질문", 0));

        then(saveQuestionPort).should().save(argThat(question -> question.getRound() == round
            && question.getContent().equals("새 질문")
            && question.getOrderNo() == 0
            && question.getCreatorMemberId() == 99L
            && question.getLastModifiedByMemberId() == 99L
            && question.isActive()));
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 공통 질문을 비활성화할 수 있다")
    void deactivateBeforeFirstSubmission() {
        RecruitingRound round = authorizedRound();
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            round,
            "기존 질문",
            0,
            10L
        );
        given(loadQuestionPort.getById(101L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(false);

        sut.deactivateRoundQuestion(DeactivateRecruitingRoundInterviewQuestionCommand.of(101L, 1L, 99L));

        assertThat(question.isActive()).isFalse();
        assertThat(question.getLastModifiedByMemberId()).isEqualTo(99L);
        then(saveQuestionPort).should().save(question);
    }

    private RecruitingRound authorizedRound() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(concurrencyLockService.lockRound(1L)).willReturn(round);
        return round;
    }
}
