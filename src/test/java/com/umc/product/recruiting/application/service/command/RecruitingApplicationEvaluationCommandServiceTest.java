package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationEvaluationCommandServiceTest {

    @Mock
    LoadRecruitingApplicationEvaluationPort loadEvaluationPort;

    @Mock
    SaveRecruitingApplicationEvaluationPort saveEvaluationPort;

    @Mock
    GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingApplicationEvaluationCommandService sut;

    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingApplicationEvaluationCommandService(
            loadEvaluationPort,
            saveEvaluationPort,
            getRoundEvaluatorUseCase,
            concurrencyLockService
        );
        application = application();
        given(concurrencyLockService.lockRoundThenApplication(900L)).willReturn(application);
    }

    @Test
    @DisplayName("평가자 whitelist에 등록된 회원은 평가 초안을 저장한다")
    void 평가자_whitelist에_등록된_회원은_평가_초안을_저장한다() {
        moveApplicationTo(RecruitingEvaluatorStage.DOCUMENT);
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L))
            .willReturn(true);
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberIdAndStage(
            900L,
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        )).willReturn(Optional.empty());
        given(saveEvaluationPort.saveEvaluation(org.mockito.ArgumentMatchers.any()))
            .willAnswer(invocation -> {
                RecruitingApplicationEvaluation evaluation = invocation.getArgument(0);
                ReflectionTestUtils.setField(evaluation, "id", 1000L);
                return evaluation;
            });

        Long evaluationId = sut.saveDraft(SaveRecruitingApplicationEvaluationCommand.of(
            900L,
            20L,
            RecruitingEvaluatorStage.DOCUMENT,
            null,
            "검토 중"
        ));

        assertThat(evaluationId).isEqualTo(1000L);
    }

    @Test
    @DisplayName("평가자 whitelist에 없는 회원은 평가를 만들 수 없다")
    void 평가자_whitelist에_없는_회원은_평가를_만들_수_없다() {
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L))
            .willReturn(false);

        assertThatThrownBy(() -> sut.saveDraft(SaveRecruitingApplicationEvaluationCommand.of(
            900L,
            20L,
            RecruitingEvaluatorStage.DOCUMENT,
            RecruitingApplicationEvaluationDecision.APPROVED,
            null
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_ACCESS_DENIED);
        verify(saveEvaluationPort, never()).saveEvaluation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("제출한 평가는 command service에서도 다시 저장할 수 없다")
    void 제출한_평가는_command_service에서도_다시_저장할_수_없다() {
        moveApplicationTo(RecruitingEvaluatorStage.INTERVIEW);
        RecruitingApplicationEvaluation submitted = RecruitingApplicationEvaluation.createDraft(
            application,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        submitted.submit(RecruitingApplicationEvaluationDecision.APPROVED, "제출");
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L))
            .willReturn(true);
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberIdAndStage(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(Optional.of(submitted));

        assertThatThrownBy(() -> sut.saveDraft(SaveRecruitingApplicationEvaluationCommand.of(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW,
            RecruitingApplicationEvaluationDecision.REJECTED,
            "변경"
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
        verify(saveEvaluationPort, never()).saveEvaluation(submitted);
    }

    @Test
    @DisplayName("평가자는 결정이 있는 초안을 제출한다")
    void 평가자는_결정이_있는_초안을_제출한다() {
        moveApplicationTo(RecruitingEvaluatorStage.INTERVIEW);
        RecruitingApplicationEvaluation draft = RecruitingApplicationEvaluation.createDraft(
            application,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L))
            .willReturn(true);
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberIdAndStage(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(Optional.of(draft));

        sut.submit(SubmitRecruitingApplicationEvaluationCommand.of(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW,
            RecruitingApplicationEvaluationDecision.APPROVED,
            "추가 논의"
        ));

        assertThat(draft.getStatus()).isEqualTo(RecruitingApplicationEvaluationStatus.SUBMITTED);
        verify(saveEvaluationPort).saveEvaluation(draft);
    }

    @Test
    @DisplayName("INTERVIEW 배정 전에는 면접 평가를 저장할 수 없다")
    void 면접_배정_전에는_면접_평가를_저장할_수_없다() {
        application.submit(1L);
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L))
            .willReturn(true);

        assertThatThrownBy(() -> sut.saveDraft(SaveRecruitingApplicationEvaluationCommand.of(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW,
            RecruitingApplicationEvaluationDecision.APPROVED,
            null
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID);
        verify(saveEvaluationPort, never()).saveEvaluation(org.mockito.ArgumentMatchers.any());
    }

    private void moveApplicationTo(RecruitingEvaluatorStage stage) {
        application.submit(1L);
        if (stage == RecruitingEvaluatorStage.INTERVIEW) {
            application.passDocument(2L, null);
            application.assignInterview(2L, null);
        }
    }

    private RecruitingApplication application() {
        RecruitingRound round = RecruitingRound.createRegular(
            RecruitingSeason.create(9L, 1L),
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                false,
                null,
                null,
                Instant.parse("2026-08-16T00:00:00Z"),
                null,
                null,
                null
            )
        );
        ReflectionTestUtils.setField(round, "id", 800L);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 100L);
        RecruitingApplication result = RecruitingApplication.createMemberDraft(
            form,
            200L,
            1L,
            RecruitingApplicantProfile.create(
                round,
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(result, "id", 900L);
        return result;
    }
}
