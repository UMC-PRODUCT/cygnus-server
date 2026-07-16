package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
class RecruitingPublicApplicationQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    GetFormResponseUseCase getFormResponseUseCase;

    @Mock
    Clock clock;

    @InjectMocks
    RecruitingPublicApplicationQueryService sut;

    @Test
    @DisplayName("최종 발표 전에는 최종 결과와 합격 트랙을 숨긴다")
    void hideFinalResultBeforePublication() {
        RecruitingApplication application = finalPassedApplication();
        given(clock.instant()).willReturn(Instant.parse("2026-08-15T23:59:59Z"));
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(Optional.of(application));
        given(getFormResponseUseCase.getResponseWithAnswersByAccessKey("raw-form-key"))
            .willReturn(formResponse());

        RecruitingPublicApplicationInfo result = sut.getByCredential(" Applicant@Example.COM ", "A1B2C3");

        assertThat(result.documentResult()).isEqualTo(RecruitingPublicResultStatus.APPROVED);
        assertThat(result.finalResult()).isEqualTo(RecruitingPublicResultStatus.PENDING);
        assertThat(result.acceptedTrack()).isNull();
    }

    @Test
    @DisplayName("최종 발표 시각부터 최종 결과와 합격 트랙을 공개한다")
    void exposeFinalResultAtPublicationBoundary() {
        RecruitingApplication application = finalPassedApplication();
        given(clock.instant()).willReturn(Instant.parse("2026-08-16T00:00:00Z"));
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(Optional.of(application));
        given(getFormResponseUseCase.getResponseWithAnswersByAccessKey("raw-form-key"))
            .willReturn(formResponse());

        RecruitingPublicApplicationInfo result = sut.getByCredential("applicant@example.com", "A1B2C3");

        assertThat(result.finalResult()).isEqualTo(RecruitingPublicResultStatus.APPROVED);
        assertThat(result.acceptedTrack()).isEqualTo(ChallengerTrack.PLAN);
    }

    @Test
    @DisplayName("지원 키 형식이 잘못되면 지원서 조회 전에 거부한다")
    void rejectInvalidApplicationKey() {
        assertThatThrownBy(() -> sut.getByCredential("applicant@example.com", "invalid"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);
    }

    private RecruitingApplication finalPassedApplication() {
        RecruitingApplicationForm form = publishedForm();
        RecruitingApplication application = RecruitingApplication.createAnonymousDraft(
            form,
            700L,
            "raw-form-key",
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3",
            77L,
            Instant.parse("2026-07-15T00:00:00Z")
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submitAnonymous("applicant@example.com");
        application.passDocument(10L, "서류 합격");
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

    private FormResponseWithAnswersInfo formResponse() {
        return FormResponseWithAnswersInfo.builder()
            .id(700L)
            .formId(500L)
            .status(FormResponseStatus.SUBMITTED)
            .answers(List.of())
            .build();
    }
}
