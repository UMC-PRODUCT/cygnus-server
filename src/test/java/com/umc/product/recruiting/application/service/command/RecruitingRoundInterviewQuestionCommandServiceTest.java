package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingInterviewQuestionsCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundInterviewQuestionCommandServiceTest {

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

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
        RecruitingInterviewQuestionMutationPolicy mutationPolicy =
            new RecruitingInterviewQuestionMutationPolicy(loadSubmittedEvaluationPort);
        sut = new RecruitingRoundInterviewQuestionCommandService(
            loadQuestionPort,
            saveQuestionPort,
            authorizeManagementUseCase,
            mutationPolicy,
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("최초 면접 평가 제출 전에는 공통 질문을 수정할 수 있다")
    void updateBeforeFirstSubmission() {
        RecruitingRound round = authorizedRound(1L, 11L);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            round,
            "기존 질문",
            0,
            10L
        );
        given(loadQuestionPort.getById(101L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(false);

        sut.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            101L,
            1L,
            99L,
            "수정 질문",
            1
        ));

        assertThat(question.getContent()).isEqualTo("수정 질문");
        assertThat(question.getOrderNo()).isEqualTo(1);
        assertThat(question.getCreatorMemberId()).isEqualTo(10L);
        assertThat(question.getLastModifiedByMemberId()).isEqualTo(99L);
        then(saveQuestionPort).should().save(question);
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 공통 질문 생성을 거부한다")
    void rejectCreationAfterFirstSubmission() {
        RecruitingRound round = authorizedRound(1L, 11L);
        given(concurrencyLockService.lockRound(1L)).willReturn(round);
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.createRoundQuestion(
            CreateRecruitingRoundInterviewQuestionCommand.of(1L, 99L, "새 질문", 0)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        then(saveQuestionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 공통 질문 수정을 거부한다")
    void rejectUpdateAfterFirstSubmission() {
        RecruitingRound round = authorizedRound(1L, 11L);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            round,
            "기존 질문",
            0,
            10L
        );
        given(loadQuestionPort.getById(101L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            101L,
            1L,
            99L,
            "수정 질문",
            1
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(question.getContent()).isEqualTo("기존 질문");
        then(saveQuestionPort).should(never()).save(question);
    }

    @Test
    @DisplayName("첫 면접 평가 제출 후에는 공통 질문 비활성화를 거부한다")
    void rejectDeactivationAfterFirstSubmission() {
        RecruitingRound round = authorizedRound(1L, 11L);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            round,
            "기존 질문",
            0,
            10L
        );
        given(loadQuestionPort.getById(101L)).willReturn(question);
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.deactivateRoundQuestion(
            DeactivateRecruitingRoundInterviewQuestionCommand.of(101L, 1L, 99L)
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(question.isActive()).isTrue();
        then(saveQuestionPort).should(never()).save(question);
    }

    @Test
    @DisplayName("모집 관리 권한이 없으면 공통 질문을 수정할 수 없다")
    void rejectUpdateWithoutManagementPermission() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(concurrencyLockService.lockRound(1L)).willReturn(round);
        willThrow(new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED))
            .given(authorizeManagementUseCase).authorizeSeasonManagement(99L, 11L);

        assertThatThrownBy(() -> sut.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            101L,
            1L,
            99L,
            "수정 질문",
            1
        ))).isInstanceOf(AuthorizationDomainException.class);
        then(loadSubmittedEvaluationPort).shouldHaveNoInteractions();
        then(saveQuestionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 차수와 질문 차수가 다르면 공통 질문 수정을 거부한다")
    void rejectUpdateForDifferentRoundScope() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound requestedRound = mock(RecruitingRound.class);
        given(requestedRound.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(concurrencyLockService.lockRound(1L)).willReturn(requestedRound);
        RecruitingRound questionRound = mock(RecruitingRound.class);
        given(questionRound.getId()).willReturn(2L);
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            questionRound,
            "기존 질문",
            0,
            10L
        );
        given(loadQuestionPort.getById(101L)).willReturn(question);

        assertThatThrownBy(() -> sut.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            101L,
            1L,
            99L,
            "수정 질문",
            1
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        then(loadSubmittedEvaluationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("공통 질문 전체 교체는 수정·생성·재활성화·누락 비활성화를 diff로 반영한다")
    void replaceRoundQuestionsAppliesFullDiff() {
        RecruitingRound round = authorizedRound(1L, 11L);
        RecruitingRoundInterviewQuestion updated = question(round, 101L, "기존 질문", 0, true);
        RecruitingRoundInterviewQuestion removed = question(round, 102L, "삭제 질문", 1, true);
        RecruitingRoundInterviewQuestion reactivated = question(round, 103L, "비활성 질문", 2, false);
        given(loadQuestionPort.listByRoundId(1L)).willReturn(List.of(updated, removed, reactivated));
        given(loadSubmittedEvaluationPort.existsSubmittedByRoundId(1L)).willReturn(false);

        sut.replaceRoundQuestions(new ReplaceRecruitingInterviewQuestionsCommand.Round(
            1L,
            99L,
            List.of(
                new ReplaceRecruitingInterviewQuestionsCommand.Entry(101L, "수정 질문", 2),
                new ReplaceRecruitingInterviewQuestionsCommand.Entry(103L, "재활성 질문", 0),
                new ReplaceRecruitingInterviewQuestionsCommand.Entry(null, "신규 질문", 1)
            )
        ));

        assertThat(updated.getContent()).isEqualTo("수정 질문");
        assertThat(removed.isActive()).isFalse();
        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.getContent()).isEqualTo("재활성 질문");
        then(saveQuestionPort).should(times(4)).save(org.mockito.ArgumentMatchers.any());
    }

    private RecruitingRound authorizedRound(Long roundId, Long seasonId) {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(roundId);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(seasonId);
        given(concurrencyLockService.lockRound(roundId)).willReturn(round);
        return round;
    }

    private RecruitingRoundInterviewQuestion question(
        RecruitingRound round,
        Long id,
        String content,
        int orderNo,
        boolean active
    ) {
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(round, content, orderNo, 10L);
        ReflectionTestUtils.setField(question, "id", id);
        if (!active) {
            question.deactivateBeforeFirstEvaluationSubmission(10L);
        }
        return question;
    }
}
