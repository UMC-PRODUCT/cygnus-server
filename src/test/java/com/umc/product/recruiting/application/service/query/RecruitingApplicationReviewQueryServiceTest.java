package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
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

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationReviewQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @Mock
    LoadRecruitingApplicationEvaluationPort loadEvaluationPort;
    @Mock
    LoadRecruitingRoundPort loadRoundPort;
    @Mock
    GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    @Mock
    GetFormResponseUseCase getFormResponseUseCase;

    RecruitingApplicationReviewQueryService sut;
    RecruitingRound round;
    RecruitingApplication application;

    @BeforeEach
    void setUp() {
        sut = new RecruitingApplicationReviewQueryService(
            loadApplicationPort,
            loadEvaluationPort,
            loadRoundPort,
            getRoundEvaluatorUseCase,
            authorizeManagementUseCase,
            getFormResponseUseCase
        );
        round = round();
        application = submittedApplication(round);
    }

    @Test
    @DisplayName("Round 평가자는 제출 지원서와 자신의 전형별 평가 여부를 페이지로 조회한다")
    void evaluatorSearchesSubmittedApplications() {
        PageRequest pageable = PageRequest.of(0, 20);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(getRoundEvaluatorUseCase.canEvaluate(20L, 99L)).willReturn(true);
        given(loadApplicationPort.searchByRoundId(
            20L,
            Set.of(),
            Set.of(ChallengerTrack.PLAN),
            pageable
        )).willReturn(new PageImpl<>(List.of(application), pageable, 1));
        given(loadEvaluationPort.listByApplicationIdsAndEvaluatorMemberId(List.of(40L), 99L))
            .willReturn(List.of(RecruitingApplicationEvaluation.create(
                application,
                99L,
                RecruitingEvaluatorStage.DOCUMENT,
                RecruitingApplicationEvaluationDecision.APPROVED,
                "검토 완료"
            )));

        var result = sut.search(RecruitingApplicationSearchQuery.builder()
            .roundId(20L)
            .tracks(Set.of(ChallengerTrack.PLAN))
            .requesterMemberId(99L)
            .pageable(pageable)
            .build());

        assertThat(result).singleElement().satisfies(info -> {
            assertThat(info.applicationId()).isEqualTo(40L);
            assertThat(info.documentEvaluatedByMe()).isTrue();
            assertThat(info.interviewEvaluatedByMe()).isFalse();
        });
    }

    @Test
    @DisplayName("평가자나 Season 관리자가 아니면 지원서 상세를 조회할 수 없다")
    void rejectUnauthorizedDetailAccess() {
        given(loadApplicationPort.getRoundIdByApplicationId(40L)).willReturn(20L);
        given(loadApplicationPort.listByIdsWithDetails(Set.of(40L))).willReturn(List.of(application));
        given(getRoundEvaluatorUseCase.canEvaluate(20L, 99L)).willReturn(false);
        given(authorizeManagementUseCase.canManageSeason(99L, 10L)).willReturn(false);

        assertThatThrownBy(() -> sut.getDetail(20L, 40L, 99L))
            .isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("지원서 상세는 Form 응답 답변을 결합하고 credential은 노출하지 않는다")
    void getMemberApplicationDetailWithAnswers() {
        given(loadApplicationPort.getRoundIdByApplicationId(40L)).willReturn(20L);
        given(getRoundEvaluatorUseCase.canEvaluate(20L, 99L)).willReturn(true);
        given(loadApplicationPort.listByIdsWithDetails(Set.of(40L))).willReturn(List.of(application));
        given(loadEvaluationPort.listByApplicationIdsAndEvaluatorMemberId(List.of(40L), 99L))
            .willReturn(List.of());
        given(getFormResponseUseCase.findResponsesWithAnswers(Set.of(30L)))
            .willReturn(Map.of(30L, FormResponseWithAnswersInfo.builder()
                .id(30L)
                .formId(100L)
                .status(FormResponseStatus.SUBMITTED)
                .answers(List.of())
                .build()));
        given(getFormResponseUseCase.findAnonymousResponsesWithAnswers(Map.of())).willReturn(Map.of());

        RecruitingApplicationDetailInfo result = sut.getDetail(20L, 40L, 99L);

        assertThat(result.formResponseId()).isEqualTo(30L);
        assertThat(result.application().email()).isEqualTo("applicant@example.com");
        assertThat(result.answers()).isEmpty();
    }

    private RecruitingRound round() {
        RecruitingSeason season = RecruitingSeason.create(1L, 2L);
        ReflectionTestUtils.setField(season, "id", 10L);
        RecruitingRound result = RecruitingRound.createRegular(
            season,
            "15기 본모집",
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.PLAN),
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
        ReflectionTestUtils.setField(result, "id", 20L);
        return result;
    }

    private RecruitingApplication submittedApplication(RecruitingRound recruitingRound) {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(recruitingRound, 100L);
        RecruitingApplication result = RecruitingApplication.createMemberDraft(
            form,
            30L,
            50L,
            RecruitingApplicantProfile.create(
                recruitingRound,
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                null
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(result, "id", 40L);
        result.submit(50L);
        return result;
    }
}
