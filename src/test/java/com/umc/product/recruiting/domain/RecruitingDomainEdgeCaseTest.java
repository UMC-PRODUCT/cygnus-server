package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting domain edge case")
class RecruitingDomainEdgeCaseTest {

    @Test
    @DisplayName("평가와 면접 질문은 null 식별자·대상·순서를 거부한다")
    void rejectInvalidEvaluationAndInterviewQuestionIdentity() {
        assertThatThrownBy(() -> RecruitingApplicationEvaluation.create(
            null,
            1L,
            RecruitingEvaluatorStage.DOCUMENT,
            RecruitingApplicationEvaluationDecision.APPROVED,
            null
        )).isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingRoundInterviewQuestion.create(null, "질문", 0, 1L))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingRoundInterviewQuestion.create(configuredRound(), "질문", null, 1L))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplicationInterviewQuestion.create(null, "질문", 0))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplicationInterviewQuestion.create(
            mock(RecruitingApplication.class), " ", 0
        )).isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("지원자 이메일과 profile은 null 및 모집하지 않는 1지망을 거부한다")
    void rejectInvalidApplicantIdentityAndChoice() {
        assertThatThrownBy(() -> RecruitingApplicantEmail.from(null))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplicantProfile.create(
            null,
            "홍길동",
            RecruitingApplicantEmail.from("applicant@example.com"),
            ChallengerTrack.PLAN,
            null
        )).isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplicantProfile.create(
            configuredRound(),
            "홍길동",
            RecruitingApplicantEmail.from("applicant@example.com"),
            ChallengerTrack.DESIGN,
            null
        )).isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("지원서 생성은 필수값·키·익명 동의·회원 access key 불변식을 지킨다")
    void rejectInvalidApplicationCreation() throws Exception {
        RecruitingApplicationForm form = applicationForm();
        RecruitingApplicantProfile profile = profile(form.getRound());
        assertThatThrownBy(() -> RecruitingApplication.createMemberDraft(null, 1L, 2L, profile, "A1B2C3"))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplication.createMemberDraft(form, 1L, 2L, profile, "bad"))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplication.createAnonymousDraft(
            form, 1L, "raw-key", profile, "A1B2C3", null, null
        )).isInstanceOf(RecruitingDomainException.class);

        Constructor<RecruitingApplication> constructor = RecruitingApplication.class.getDeclaredConstructor(
            RecruitingApplicationForm.class,
            Long.class,
            String.class,
            Long.class,
            RecruitingApplicantProfile.class,
            String.class,
            Long.class,
            Instant.class
        );
        constructor.setAccessible(true);
        assertThatThrownBy(() -> constructor.newInstance(
            form, 1L, "member-must-not-have-access-key", 2L, profile, "A1B2C3", null, null
        )).hasCauseInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("지원서 상태 전이와 수정·등록은 잘못된 상태 및 null profile을 거부한다")
    void rejectInvalidApplicationTransitions() {
        RecruitingApplication submitted = memberDraft();
        submitted.submit(2L);
        assertThatThrownBy(() -> submitted.submit(2L)).isInstanceOf(RecruitingDomainException.class);

        RecruitingApplication cancelled = memberDraft();
        cancelled.cancel(2L, "철회");
        assertThatThrownBy(() -> cancelled.updateDraft(2L, profile(cancelled.getRound())))
            .isInstanceOf(RecruitingDomainException.class);

        RecruitingApplication editable = memberDraft();
        assertThatThrownBy(() -> editable.updateDraft(2L, null))
            .isInstanceOf(RecruitingDomainException.class);

        RecruitingApplication anonymous = anonymousDraft();
        assertThatThrownBy(() -> anonymous.validateAnonymousApplicant("other@example.com"))
            .isInstanceOf(RecruitingDomainException.class);

        RecruitingApplication missingAcceptedTrack = memberDraft();
        ReflectionTestUtils.setField(
            missingAcceptedTrack, "status", RecruitingApplicationStatus.FINAL_PASSED
        );
        assertThatThrownBy(() -> missingAcceptedTrack.markRegistrationReady(2L))
            .isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("Round·Form·section policy는 null 일정과 대상 및 잘못된 상태를 거부한다")
    void rejectInvalidRoundFormAndPolicy() {
        RecruitingRound round = configuredRound();
        assertThatThrownBy(() -> round.updateConfiguration(null, false))
            .isInstanceOf(RecruitingDomainException.class);
        assertThat(round.isLocalApplicationPeriodOpenAt(null, null)).isFalse();
        assertThatThrownBy(() -> RecruitingRound.normalizeTitle(null))
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingApplicationForm.create(null, 1L))
            .isInstanceOf(RecruitingDomainException.class);

        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 1L);
        assertThatThrownBy(form::close).isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> RecruitingFormSectionPolicy.createCommon(null, 1L))
            .isInstanceOf(RecruitingDomainException.class);
        RecruitingFormSectionPolicy policy = RecruitingFormSectionPolicy.createCommon(form, 1L);
        assertThatThrownBy(() -> policy.updatePolicy(null, null))
            .isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("도메인 예외는 override message를 보존한다")
    void createDomainExceptionWithCustomMessage() {
        RecruitingDomainException exception = new RecruitingDomainException(
            RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND,
            "지원서를 찾지 못했습니다"
        );

        assertThat(exception.getMessage()).isEqualTo("지원서를 찾지 못했습니다");
    }

    @Test
    @DisplayName("Round 생성·수정 command는 configuration 누락을 도메인 경계에서 거부한다")
    void rejectRoundCommandWithoutConfiguration() {
        assertThatThrownBy(() -> CreateRecruitingRoundCommand.builder().configuration(null).build())
            .isInstanceOf(RecruitingDomainException.class);
        assertThatThrownBy(() -> UpdateRecruitingRoundCommand.builder().configuration(null).build())
            .isInstanceOf(RecruitingDomainException.class);
    }

    private RecruitingApplication memberDraft() {
        RecruitingApplicationForm form = applicationForm();
        return RecruitingApplication.createMemberDraft(form, 1L, 2L, profile(form.getRound()), "A1B2C3");
    }

    private RecruitingApplication anonymousDraft() {
        RecruitingApplicationForm form = applicationForm();
        return RecruitingApplication.createAnonymousDraft(
            form,
            1L,
            "raw-key",
            profile(form.getRound()),
            "A1B2C3",
            3L,
            Instant.parse("2026-07-01T00:00:00Z")
        );
    }

    private RecruitingApplicantProfile profile(RecruitingRound round) {
        return RecruitingApplicantProfile.create(
            round,
            "홍길동",
            RecruitingApplicantEmail.from("applicant@example.com"),
            ChallengerTrack.PLAN,
            null
        );
    }

    private RecruitingApplicationForm applicationForm() {
        return RecruitingApplicationForm.create(configuredRound(), 100L);
    }

    private RecruitingRound configuredRound() {
        return RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L), RecruitingRoundConfiguration.of(
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
    }
}
