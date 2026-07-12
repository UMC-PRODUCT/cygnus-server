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

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

@ExtendWith(MockitoExtension.class)
class RecruitingEvaluatorQuestionQueryServiceTest {

    @Mock
    LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;

    @Mock
    LoadRecruitingRoundInterviewQuestionPort loadRoundQuestionPort;

    @Mock
    LoadRecruitingApplicationInterviewQuestionPort loadApplicationQuestionPort;

    @InjectMocks
    RecruitingRoundEvaluatorQueryService evaluatorService;

    @InjectMocks
    RecruitingInterviewQuestionQueryService questionService;

    @Test
    @DisplayName("DOCUMENT whitelist가 있으면 서류 평가 권한이 있다")
    void canEvaluateDocumentForDocumentWhitelist() {
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            1L,
            10L,
            RecruitingEvaluatorStage.DOCUMENT
        )).willReturn(true);

        boolean result = evaluatorService.canEvaluate(1L, 10L, RecruitingEvaluatorStage.DOCUMENT);

        assertThat(result).isTrue();
        then(loadEvaluatorPort).should().existsByRoundIdAndMemberIdAndStage(
            1L,
            10L,
            RecruitingEvaluatorStage.DOCUMENT
        );
    }

    @Test
    @DisplayName("INTERVIEW whitelist가 없으면 면접 평가 권한이 없다")
    void cannotEvaluateInterviewWithoutInterviewWhitelist() {
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            1L,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(false);

        boolean result = evaluatorService.canEvaluate(1L, 10L, RecruitingEvaluatorStage.INTERVIEW);

        assertThat(result).isFalse();
        then(loadEvaluatorPort).should().existsByRoundIdAndMemberIdAndStage(
            1L,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        );
    }

    @Test
    @DisplayName("평가자 목록은 요청한 평가 단계만 반환한다")
    void listEvaluatorsByStage() {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        RecruitingRoundEvaluator evaluator = RecruitingRoundEvaluator.create(
            round,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        ReflectionTestUtils.setField(evaluator, "id", 100L);
        given(loadEvaluatorPort.listByRoundIdAndStage(1L, RecruitingEvaluatorStage.INTERVIEW))
            .willReturn(List.of(evaluator));

        List<RecruitingRoundEvaluatorInfo> result = evaluatorService.listByRoundIdAndStage(
            1L,
            RecruitingEvaluatorStage.INTERVIEW
        );

        assertThat(result).singleElement().satisfies(info -> {
            assertThat(info.roundId()).isEqualTo(1L);
            assertThat(info.memberId()).isEqualTo(10L);
            assertThat(info.stage()).isEqualTo(RecruitingEvaluatorStage.INTERVIEW);
        });
    }

    @Test
    @DisplayName("공통 질문은 활성 항목만 순서대로 조회한다")
    void listActiveRoundQuestions() {
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getId()).willReturn(1L);
        RecruitingRoundInterviewQuestion roundQuestion = RecruitingRoundInterviewQuestion.create(round, "공통", 0);
        given(loadRoundQuestionPort.listActiveByRoundId(1L)).willReturn(List.of(roundQuestion));

        List<RecruitingRoundInterviewQuestionInfo> commonResult = questionService.listActiveRoundQuestions(1L);

        assertThat(commonResult).extracting(RecruitingRoundInterviewQuestionInfo::content).containsExactly("공통");
    }

    @Test
    @DisplayName("개별 질문은 활성 항목만 순서대로 조회한다")
    void listActiveApplicationQuestions() {
        RecruitingApplication application = mock(RecruitingApplication.class);
        given(application.getId()).willReturn(2L);
        RecruitingApplicationInterviewQuestion applicationQuestion = RecruitingApplicationInterviewQuestion.create(
            application,
            "개별",
            1
        );
        given(loadApplicationQuestionPort.listActiveByApplicationId(2L)).willReturn(List.of(applicationQuestion));

        List<RecruitingApplicationInterviewQuestionInfo> individualResult =
            questionService.listActiveApplicationQuestions(2L);

        assertThat(individualResult)
            .extracting(RecruitingApplicationInterviewQuestionInfo::content)
            .containsExactly("개별");
    }
}
