package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationEvaluationQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    LoadRecruitingApplicationEvaluationPort loadEvaluationPort;

    @Mock
    GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    RecruitingApplicationEvaluationQueryService sut;

    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingApplicationEvaluationQueryService(
            loadApplicationPort,
            loadEvaluationPort,
            getRoundEvaluatorUseCase,
            checkPermissionUseCase
        );
        application = application();
        given(loadApplicationPort.getById(900L)).willReturn(application);
    }

    @Test
    @DisplayName("본인 평가 제출 전에는 같은 단계의 본인 평가만 조회한다")
    void 본인_평가_제출_전에는_같은_단계의_본인_평가만_조회한다() {
        RecruitingApplicationEvaluation own = evaluation(20L, RecruitingEvaluatorStage.INTERVIEW);
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L, RecruitingEvaluatorStage.INTERVIEW))
            .willReturn(true);
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberIdAndStage(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(Optional.of(own));

        List<RecruitingApplicationEvaluationInfo> result = sut.listVisibleEvaluations(
            900L,
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        );

        assertThat(result).extracting(RecruitingApplicationEvaluationInfo::evaluatorMemberId).containsExactly(20L);
    }

    @Test
    @DisplayName("본인 평가 제출 후에는 같은 단계의 다른 평가를 조회한다")
    void 본인_평가_제출_후에는_같은_단계의_다른_평가를_조회한다() {
        RecruitingApplicationEvaluation own = evaluation(20L, RecruitingEvaluatorStage.DOCUMENT);
        own.submit(RecruitingApplicationEvaluationDecision.PASS, "제출");
        RecruitingApplicationEvaluation peer = evaluation(21L, RecruitingEvaluatorStage.DOCUMENT);
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 20L, RecruitingEvaluatorStage.DOCUMENT))
            .willReturn(true);
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberIdAndStage(
            900L,
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        )).willReturn(Optional.of(own));
        given(loadEvaluationPort.listByApplicationIdAndStage(900L, RecruitingEvaluatorStage.DOCUMENT))
            .willReturn(List.of(own, peer));

        List<RecruitingApplicationEvaluationInfo> result = sut.listVisibleEvaluations(
            900L,
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        assertThat(result)
            .extracting(RecruitingApplicationEvaluationInfo::evaluatorMemberId)
            .containsExactly(20L, 21L);
    }

    @Test
    @DisplayName("학교와 중앙 권한자는 whitelist와 제출 여부를 우회한다")
    void 학교와_중앙_권한자는_whitelist와_제출_여부를_우회한다() {
        RecruitingApplicationEvaluation first = evaluation(20L, RecruitingEvaluatorStage.DOCUMENT);
        RecruitingApplicationEvaluation second = evaluation(21L, RecruitingEvaluatorStage.DOCUMENT);
        given(checkPermissionUseCase.check(
            99L,
            ResourcePermission.of(ResourceType.RECRUITMENT, 700L, PermissionType.READ)
        )).willReturn(true);
        given(loadEvaluationPort.listByApplicationIdAndStage(900L, RecruitingEvaluatorStage.DOCUMENT))
            .willReturn(List.of(first, second));

        List<RecruitingApplicationEvaluationInfo> result = sut.listVisibleEvaluations(
            900L,
            99L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("whitelist에 없는 일반 회원은 평가를 조회할 수 없다")
    void whitelist에_없는_일반_회원은_평가를_조회할_수_없다() {
        given(getRoundEvaluatorUseCase.canEvaluate(800L, 99L, RecruitingEvaluatorStage.INTERVIEW))
            .willReturn(false);

        assertThatThrownBy(() -> sut.listVisibleEvaluations(
            900L,
            99L,
            RecruitingEvaluatorStage.INTERVIEW
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_ACCESS_DENIED);
    }

    private RecruitingApplicationEvaluation evaluation(Long evaluatorMemberId, RecruitingEvaluatorStage stage) {
        return RecruitingApplicationEvaluation.createDraft(application, evaluatorMemberId, stage);
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
        ReflectionTestUtils.setField(round.getSeason(), "id", 700L);
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
