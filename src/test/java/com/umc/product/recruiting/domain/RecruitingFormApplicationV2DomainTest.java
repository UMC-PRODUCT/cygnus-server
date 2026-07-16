package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting v2 지원 폼과 지원서 도메인")
class RecruitingFormApplicationV2DomainTest {

    @Test
    @DisplayName("지원 폼은 트랙 없이 모집 차수당 하나의 Form을 연결한다")
    void createSingleFormForRound() {
        RecruitingRound round = configuredRound(true);

        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 100L);

        assertThat(form.getRound()).isSameAs(round);
        assertThat(form.getFormId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("공통 섹션은 트랙이 없고 트랙 섹션은 모집 차수 트랙만 허용한다")
    void validateSectionPolicyTrack() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(configuredRound(true), 100L);

        RecruitingFormSectionPolicy common = RecruitingFormSectionPolicy.createCommon(form, 10L);
        RecruitingFormSectionPolicy track = RecruitingFormSectionPolicy.createTrack(
            form,
            11L,
            ChallengerTrack.PLAN
        );

        assertThat(common.getType()).isEqualTo(RecruitingFormSectionType.COMMON);
        assertThat(common.getTrack()).isNull();
        assertThat(track.getType()).isEqualTo(RecruitingFormSectionType.TRACK);
        assertThat(track.getTrack()).isEqualTo(ChallengerTrack.PLAN);
        assertThatThrownBy(() -> RecruitingFormSectionPolicy.createTrack(
            form,
            12L,
            ChallengerTrack.INFRA_PLUS
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK);
        assertThatThrownBy(() -> RecruitingFormSectionPolicy.createTrack(
            form,
            13L,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK);
    }

    @Test
    @DisplayName("지원 폼 게시 검증은 모집 차수의 모든 트랙 섹션을 요구한다")
    void requireEveryRecruitableTrackSectionBeforePublish() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(configuredRound(true), 100L);
        assertThatThrownBy(() -> form.publish(List.of(ChallengerTrack.PLAN)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_TRACK_SECTION_REQUIRED);

        form.publish(List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN));

        assertThat(form.getStatus()).isEqualTo(RecruitingApplicationFormStatus.PUBLISHED);
    }

    @Test
    @DisplayName("회원 지원서는 이메일을 정규화하고 개인정보 동의 없이 생성할 수 있다")
    void createMemberDraftWithNormalizedEmail() {
        RecruitingApplication application = memberDraft(
            "홍길동",
            "  Applicant@Example.COM  ",
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        );

        assertThat(application.getApplicantName()).isEqualTo("홍길동");
        assertThat(application.getApplicantEmail()).isEqualTo("applicant@example.com");
        assertThat(application.getPrivacyTermId()).isNull();
        assertThat(application.getPrivacyAgreedAt()).isNull();
        assertThat(application.getApplicationKey()).matches("[A-Z0-9]{6}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@", "a@@b.com", "a b@example.com", ".a@example.com", "a@localhost"})
    @DisplayName("실용 이메일 형식이 아니면 지원서를 생성할 수 없다")
    void rejectMalformedApplicantEmail(String malformedEmail) {
        assertThatThrownBy(() -> memberDraft(
            "홍길동",
            malformedEmail,
            ChallengerTrack.PLAN,
            null
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_EMAIL);
    }

    @Test
    @DisplayName("지원자 이름에는 공백을 사용할 수 없다")
    void rejectWhitespaceInApplicantName() {
        assertThatThrownBy(() -> memberDraft(
            "홍 길동",
            "applicant@example.com",
            ChallengerTrack.PLAN,
            null
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_APPLICANT_NAME);
    }

    @Test
    @DisplayName("2지망은 차수에서 활성화되고 1지망과 다른 모집 트랙이어야 한다")
    void validateSecondChoice() {
        assertThatThrownBy(() -> memberDraft(
            "홍길동",
            "applicant@example.com",
            ChallengerTrack.PLAN,
            ChallengerTrack.PLAN
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_SECOND_CHOICE);

        RecruitingApplicationForm disabledForm = RecruitingApplicationForm.create(configuredRound(false), 200L);
        assertThatThrownBy(() -> RecruitingApplication.createMemberDraft(
            disabledForm,
            1000L,
            2000L,
            RecruitingApplicantProfile.create(
                disabledForm.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3"
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_SECOND_CHOICE);
    }

    @Test
    @DisplayName("합격 트랙은 1지망 또는 2지망 중 하나만 허용한다")
    void validateAcceptedTrack() {
        RecruitingApplication application = memberDraft(
            "홍길동",
            "applicant@example.com",
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        );

        application.acceptTrack(ChallengerTrack.DESIGN);

        assertThat(application.getAcceptedTrack()).isEqualTo(ChallengerTrack.DESIGN);
        assertThatThrownBy(() -> application.acceptTrack(ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK);
    }

    @Test
    @DisplayName("개인정보 약관과 동의 시각은 함께 기록한다")
    void recordPrivacyConsentAsPair() {
        RecruitingApplication application = memberDraft(
            "홍길동",
            "applicant@example.com",
            ChallengerTrack.PLAN,
            null
        );
        Instant agreedAt = Instant.parse("2026-07-12T00:00:00Z");

        application.recordPrivacyConsent(10L, agreedAt);

        assertThat(application.getPrivacyTermId()).isEqualTo(10L);
        assertThat(application.getPrivacyAgreedAt()).isEqualTo(agreedAt);
        assertThatThrownBy(() -> application.recordPrivacyConsent(null, agreedAt))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT);
    }

    private RecruitingApplication memberDraft(
        String applicantName,
        String applicantEmail,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(configuredRound(true), 100L);
        return RecruitingApplication.createMemberDraft(
            form,
            1000L,
            2000L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                applicantName,
                RecruitingApplicantEmail.from(applicantEmail),
                firstChoice,
                secondChoice
            ),
            "A1B2C3"
        );
    }

    private RecruitingRound configuredRound(boolean secondChoiceEnabled) {
        return RecruitingRound.createRegular(
            RecruitingSeason.create(1L, 10L),
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
                secondChoiceEnabled,
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
    }
}
