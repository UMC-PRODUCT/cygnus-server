package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationValidationServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    GetFormResponseUseCase getFormResponseUseCase;

    @InjectMocks
    RecruitingApplicationValidationService sut;

    @Test
    @DisplayName("같은 차수의 회원 또는 이메일 지원서는 상태와 무관하게 중복을 거부한다")
    void rejectSameRoundMemberOrEmail() {
        RecruitingRound round = round();
        given(loadApplicationPort.existsByRoundIdAndApplicantMemberId(10L, 200L)).willReturn(true);

        assertThatThrownBy(() -> sut.validateNew(round, 200L, "applicant@example.com"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("같은 기수 다른 학교의 진행 중 회원 또는 이메일 지원서를 거부한다")
    void rejectBlockingApplicationAtDifferentSchool() {
        RecruitingRound round = round();
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
            1L,
            100L,
            200L,
            "applicant@example.com",
            null
        )).willReturn(true);

        assertThatThrownBy(() -> sut.validateNew(round, 200L, "applicant@example.com"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS);
    }

    @Test
    @DisplayName("같은 기수의 진행 중이거나 합격한 지원서가 있으면 재지원을 거부한다")
    void rejectBlockingReapplication() {
        RecruitingRound round = round();
        given(loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicant(
            1L,
            200L,
            "applicant@example.com",
            900L
        )).willReturn(true);

        assertThatThrownBy(() -> sut.validateUpdate(round, 200L, "applicant@example.com", 900L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_REAPPLICATION_BLOCKED);
    }

    @Test
    @DisplayName("ACTIVE 시즌과 OPEN 차수의 local 접수 기간에는 지원할 수 있다")
    void allowOpenLocalApplicationPeriod() {
        RecruitingApplicationForm form = openApplicationForm();

        assertThatCode(() -> sut.validateApplicationPeriod(
            form,
            Instant.parse("2026-08-04T00:00:00Z")
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("local 접수 종료 이후에는 지원할 수 없다")
    void rejectClosedLocalApplicationPeriod() {
        RecruitingApplicationForm form = openApplicationForm();

        assertThatThrownBy(() -> sut.validateApplicationPeriod(
            form,
            Instant.parse("2026-08-08T00:00:00.000000001Z")
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_PERIOD_CLOSED);
    }

    @Test
    @DisplayName("Form 응답의 회원과 Form 연결이 일치하면 수정할 수 있다")
    void allowOwnedLinkedFormResponse() {
        RecruitingApplication application = application();
        given(getFormResponseUseCase.findById(700L)).willReturn(java.util.Optional.of(
            formResponse(500L, 200L)
        ));

        assertThatCode(() -> sut.validateFormResponseOwnership(application, 200L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Form 응답의 회원 또는 Form 연결이 다르면 수정을 거부한다")
    void rejectMismatchedFormResponseOwnership() {
        RecruitingApplication application = application();
        given(getFormResponseUseCase.findById(700L)).willReturn(java.util.Optional.of(
            formResponse(501L, 201L)
        ));

        assertThatThrownBy(() -> sut.validateFormResponseOwnership(application, 200L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
    }

    @Test
    @DisplayName("익명 Form 응답의 access key·응답 ID·Form 연결이 모두 일치하면 수정할 수 있다")
    void allowOwnedAnonymousFormResponse() {
        RecruitingApplication application = anonymousApplication();
        given(getFormResponseUseCase.findByAccessKey("raw-key")).willReturn(java.util.Optional.of(
            formResponse(500L, null)
        ));

        assertThatCode(() -> sut.validateAnonymousFormResponseOwnership(application))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("익명 Form 응답에 회원이 연결되거나 응답 ID·Form이 다르면 수정을 거부한다")
    void rejectMismatchedAnonymousFormResponseOwnership() {
        RecruitingApplication application = anonymousApplication();
        given(getFormResponseUseCase.findByAccessKey("raw-key")).willReturn(java.util.Optional.of(
            FormResponseInfo.builder()
                .id(701L)
                .formId(501L)
                .respondentMemberId(200L)
                .status(FormResponseStatus.DRAFT)
                .build()
        ));

        assertThatThrownBy(() -> sut.validateAnonymousFormResponseOwnership(application))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
    }

    private RecruitingRound round() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 100L));
        ReflectionTestUtils.setField(round, "id", 10L);
        return round;
    }

    private RecruitingApplicationForm openApplicationForm() {
        RecruitingSeason season = RecruitingSeason.create(1L, 100L);
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
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
        ));
        round.open();
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        form.publish(form.getRound().getRecruitableTracks());
        return form;
    }

    private RecruitingApplication application() {
        RecruitingApplicationForm form = openApplicationForm();
        return RecruitingApplication.createMemberDraft(
            form,
            700L,
            200L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                null
            ),
            "A1B2C3"
        );
    }

    private RecruitingApplication anonymousApplication() {
        RecruitingApplicationForm form = openApplicationForm();
        return RecruitingApplication.createAnonymousDraft(
            form,
            700L,
            "raw-key",
            RecruitingApplicantProfile.create(
                form.getRound(),
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                null
            ),
            "A1B2C3",
            77L,
            Instant.parse("2026-07-01T00:00:00Z")
        );
    }

    private FormResponseInfo formResponse(Long formId, Long respondentMemberId) {
        return FormResponseInfo.builder()
            .id(700L)
            .formId(formId)
            .respondentMemberId(respondentMemberId)
            .status(FormResponseStatus.DRAFT)
            .build();
    }
}
