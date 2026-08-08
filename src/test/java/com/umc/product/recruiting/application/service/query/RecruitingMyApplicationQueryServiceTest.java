package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingMyApplicationQueryServiceTest {

    private static final Long MEMBER_ID = 200L;

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    GetFormResponseUseCase getFormResponseUseCase;

    @Mock
    Clock clock;

    @InjectMocks
    RecruitingMyApplicationQueryService sut;

    @Test
    @DisplayName("회원 본인 지원 내역은 Form 응답을 일괄 조회하고 비회원과 같은 공개 정책을 적용한다")
    void listMyApplicationsUsesBatchFormResponsesAndSharedVisibilityPolicy() {
        RecruitingApplication application = finalPassedApplication();
        given(loadApplicationPort.listByApplicantMemberId(MEMBER_ID)).willReturn(List.of(application));
        given(getFormResponseUseCase.findResponsesWithAnswers(Set.of(700L)))
            .willReturn(Map.of(700L, formResponse(700L, MEMBER_ID)));
        given(clock.instant()).willReturn(Instant.parse("2026-08-15T23:59:59Z"));

        List<RecruitingPublicApplicationInfo> result = sut.listMyApplications(MEMBER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).applicationId()).isEqualTo(900L);
        assertThat(result.get(0).gisuId()).isEqualTo(1L);
        assertThat(result.get(0).roundId()).isEqualTo(10L);
        assertThat(result.get(0).documentResult()).isEqualTo(RecruitingPublicResultStatus.APPROVED);
        assertThat(result.get(0).finalResult()).isEqualTo(RecruitingPublicResultStatus.PENDING);
        assertThat(result.get(0).acceptedTrack()).isNull();
        then(getFormResponseUseCase).should().findResponsesWithAnswers(Set.of(700L));
    }

    @Test
    @DisplayName("회원 본인 지원 내역이 없으면 Form 응답을 조회하지 않고 빈 목록을 반환한다")
    void returnEmptyListWithoutLoadingFormResponses() {
        given(loadApplicationPort.listByApplicantMemberId(MEMBER_ID)).willReturn(List.of());

        List<RecruitingPublicApplicationInfo> result = sut.listMyApplications(MEMBER_ID);

        assertThat(result).isEmpty();
        then(getFormResponseUseCase).shouldHaveNoInteractions();
        then(clock).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서와 연결된 기명 Form 응답이 없으면 지원서 미존재 오류로 실패한다")
    void failWhenLinkedFormResponseIsMissing() {
        RecruitingApplication application = finalPassedApplication();
        given(loadApplicationPort.listByApplicantMemberId(MEMBER_ID)).willReturn(List.of(application));
        given(getFormResponseUseCase.findResponsesWithAnswers(Set.of(700L))).willReturn(Map.of());
        given(clock.instant()).willReturn(Instant.parse("2026-08-16T00:00:00Z"));

        assertThatThrownBy(() -> sut.listMyApplications(MEMBER_ID))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
    }

    private RecruitingApplication finalPassedApplication() {
        RecruitingApplicationForm form = publishedForm();
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            700L,
            MEMBER_ID,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(MEMBER_ID);
        application.skipInterview(10L, "면접 미진행");
        application.passFinal(10L, "최종 합격", ChallengerTrack.PLAN);
        return application;
    }

    private RecruitingApplicationForm publishedForm() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
            true,
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
        ));
        ReflectionTestUtils.setField(round, "id", 10L);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish(form.getRound().getRecruitableTracks());
        return form;
    }

    private FormResponseWithAnswersInfo formResponse(Long id, Long respondentMemberId) {
        return FormResponseWithAnswersInfo.builder()
            .id(id)
            .formId(500L)
            .respondentMemberId(respondentMemberId)
            .status(FormResponseStatus.SUBMITTED)
            .answers(List.of())
            .build();
    }
}
