package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;

@ExtendWith(MockitoExtension.class)
class RecruitingEvaluatorQuestionQueryServiceTest {

    @Mock
    LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;

    @Mock
    LoadRecruitingRoundInterviewQuestionPort loadRoundQuestionPort;

    @Mock
    LoadRecruitingApplicationInterviewQuestionPort loadApplicationQuestionPort;

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @InjectMocks
    RecruitingRoundEvaluatorQueryService evaluatorService;

    @InjectMocks
    RecruitingInterviewQuestionQueryService questionService;

    @Test
    @DisplayName("차수 평가자 whitelist가 있으면 서류와 면접 평가 권한이 있다")
    void canEvaluateForRoundWhitelist() {
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 10L)).willReturn(true);

        boolean result = evaluatorService.canEvaluate(1L, 10L);

        assertThat(result).isTrue();
        then(loadEvaluatorPort).should().existsByRoundIdAndMemberId(1L, 10L);
    }

    @Test
    @DisplayName("차수 평가자 whitelist가 없으면 평가 권한이 없다")
    void cannotEvaluateWithoutRoundWhitelist() {
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 10L)).willReturn(false);

        boolean result = evaluatorService.canEvaluate(1L, 10L);

        assertThat(result).isFalse();
        then(loadEvaluatorPort).should().existsByRoundIdAndMemberId(1L, 10L);
    }

    @Test
    @DisplayName("평가자 목록은 차수에 등록된 평가자를 반환한다")
    void listEvaluatorsByRound() {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        RecruitingRoundEvaluator evaluator = RecruitingRoundEvaluator.create(round, 10L);
        ReflectionTestUtils.setField(evaluator, "id", 100L);
        given(loadEvaluatorPort.listByRoundId(1L)).willReturn(List.of(evaluator));

        List<RecruitingRoundEvaluatorInfo> result = evaluatorService.listByRoundId(1L);

        assertThat(result).singleElement().satisfies(info -> {
            assertThat(info.roundId()).isEqualTo(1L);
            assertThat(info.memberId()).isEqualTo(10L);
        });
    }

    @Test
    @DisplayName("차수 평가자는 공통 질문을 조회한다")
    void evaluatorListsActiveRoundQuestions() {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingSeason season = mock(RecruitingSeason.class);
        given(round.getId()).willReturn(1L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(9L);
        RecruitingRoundInterviewQuestion roundQuestion = RecruitingRoundInterviewQuestion.create(
            round,
            "공통",
            0,
            20L
        );
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(authorizeManagementUseCase.canManageSeason(10L, 9L)).willReturn(false);
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 10L)).willReturn(true);
        given(loadRoundQuestionPort.listActiveByRoundId(1L)).willReturn(List.of(roundQuestion));

        List<RecruitingRoundInterviewQuestionInfo> commonResult = questionService.listActiveRoundQuestions(1L, 10L);

        assertThat(commonResult).extracting(RecruitingRoundInterviewQuestionInfo::content).containsExactly("공통");
    }

    @Test
    @DisplayName("차수 평가자는 지원서의 개별 질문을 조회한다")
    void evaluatorListsActiveApplicationQuestions() {
        RecruitingApplication application = mock(RecruitingApplication.class);
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingSeason season = mock(RecruitingSeason.class);
        given(application.getId()).willReturn(2L);
        given(application.getRound()).willReturn(round);
        given(round.getId()).willReturn(1L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(9L);
        RecruitingApplicationInterviewQuestion applicationQuestion = RecruitingApplicationInterviewQuestion.create(
            application,
            "개별",
            1
        );
        given(loadApplicationPort.getById(2L)).willReturn(application);
        given(authorizeManagementUseCase.canManageSeason(10L, 9L)).willReturn(false);
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 10L)).willReturn(true);
        given(loadApplicationQuestionPort.listActiveByApplicationId(2L)).willReturn(List.of(applicationQuestion));

        List<RecruitingApplicationInterviewQuestionInfo> individualResult =
            questionService.listActiveApplicationQuestions(2L, 10L);

        assertThat(individualResult)
            .extracting(RecruitingApplicationInterviewQuestionInfo::content)
            .containsExactly("개별");
    }

    @Test
    @DisplayName("모집 관리자는 평가자 whitelist 없이 공통 질문을 조회한다")
    void managerBypassesEvaluatorWhitelistForRoundQuestions() {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingSeason season = mock(RecruitingSeason.class);
        given(round.getId()).willReturn(1L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(9L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(authorizeManagementUseCase.canManageSeason(20L, 9L)).willReturn(true);
        given(loadRoundQuestionPort.listActiveByRoundId(1L)).willReturn(List.of());

        List<RecruitingRoundInterviewQuestionInfo> result = questionService.listActiveRoundQuestions(1L, 20L);

        assertThat(result).isEmpty();
        then(loadEvaluatorPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리 권한과 평가자 whitelist가 없으면 질문 조회를 거부한다")
    void rejectQuestionReadWithoutManagementOrEvaluatorWhitelist() {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingSeason season = mock(RecruitingSeason.class);
        given(round.getId()).willReturn(1L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(9L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(authorizeManagementUseCase.canManageSeason(10L, 9L)).willReturn(false);
        given(loadEvaluatorPort.existsByRoundIdAndMemberId(1L, 10L)).willReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> questionService.listActiveRoundQuestions(1L, 10L)
        ).isInstanceOf(RecruitingDomainException.class);
        then(loadRoundQuestionPort).shouldHaveNoInteractions();
    }
}
