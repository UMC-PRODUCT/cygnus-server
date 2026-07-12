package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting core domain")
class RecruitingCoreDomainTest {

    @Test
    @DisplayName("학교와 기수를 기준으로 모집 시즌을 생성한다")
    void 학교와_기수를_기준으로_모집_시즌을_생성한다() {
        RecruitingSeason season = RecruitingSeason.create(9L, 1L);

        assertThat(season.getGisuId()).isEqualTo(9L);
        assertThat(season.getSchoolId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("본모집 차수는 1차로 고정된다")
    void 본모집_차수는_1차로_고정된다() {
        RecruitingRound round = RecruitingRound.createRegular(season());

        assertThat(round.getType()).isEqualTo(RecruitingRoundType.REGULAR);
        assertThat(round.getRoundNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("추가모집 차수는 학교별 차수 번호를 가진다")
    void 추가모집_차수는_학교별_차수_번호를_가진다() {
        RecruitingRound round = RecruitingRound.createAdditional(season(), 3);

        assertThat(round.getType()).isEqualTo(RecruitingRoundType.ADDITIONAL);
        assertThat(round.getRoundNo()).isEqualTo(3);
    }

    @Test
    @DisplayName("추가모집 차수 번호는 1 이상이어야 한다")
    void 추가모집_차수_번호는_1_이상이어야_한다() {
        assertThatThrownBy(() -> RecruitingRound.createAdditional(season(), 0))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_ROUND_NO);
    }

    @Test
    @DisplayName("지원 Form은 모집 차수와 form id를 가진다")
    void 지원_Form은_모집_차수와_form_id를_가진다() {
        RecruitingRound round = RecruitingRound.createRegular(season());

        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 100L);

        assertThat(form.getRound()).isSameAs(round);
        assertThat(form.getFormId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("지원서는 draft에서 submitted로 제출된다")
    void 지원서는_draft에서_submitted로_제출된다() {
        RecruitingApplication application = draftApplication();

        application.submit(1L);

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        assertThat(application.getSubmittedAt()).isNotNull();
        assertThat(application.getStatusChangedMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("제출된 지원서는 서류 합격 후 최종 합격과 등록 대기 상태가 분리된다")
    void 제출된_지원서는_서류_합격_후_최종_합격과_등록_대기_상태가_분리된다() {
        RecruitingApplication application = submittedApplication();

        application.passDocument(2L, "서류 합격");
        application.passFinal(3L, "최종 합격", application.getFirstChoice());
        application.markRegistrationReady(4L);

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.READY);
    }

    @Test
    @DisplayName("면접을 진행하지 않는 학교는 서류 합격 후 바로 최종 결정을 할 수 있다")
    void 면접을_진행하지_않는_학교는_서류_합격_후_바로_최종_결정을_할_수_있다() {
        RecruitingApplication application = submittedApplication();

        application.passDocument(2L, "서류 합격");
        application.skipInterview(2L, "면접 미진행");
        application.failFinal(3L, "정원 초과");

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_FAILED);
    }

    @Test
    @DisplayName("최종 합격 전에는 등록 대기 상태로 변경할 수 없다")
    void 최종_합격_전에는_등록_대기_상태로_변경할_수_없다() {
        RecruitingApplication application = submittedApplication();

        assertThatThrownBy(() -> application.markRegistrationReady(4L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("불합격과 철회만 다음 차수 재지원을 허용한다")
    void 불합격과_철회만_다음_차수_재지원을_허용한다() {
        RecruitingApplication cancelled = draftApplication();
        cancelled.cancel(1L, "철회");

        RecruitingApplication failed = submittedApplication();
        failed.failDocument(2L, "서류 불합격");

        RecruitingApplication inProgress = submittedApplication();

        assertThat(cancelled.allowsReapplication()).isTrue();
        assertThat(failed.allowsReapplication()).isTrue();
        assertThat(inProgress.blocksReapplication()).isTrue();
    }

    private RecruitingSeason season() {
        return RecruitingSeason.create(9L, 1L);
    }

    private RecruitingApplicationForm applicationForm() {
        return RecruitingApplicationForm.create(configuredRound(), 100L);
    }

    private RecruitingApplication draftApplication() {
        RecruitingApplicationForm form = applicationForm();
        return RecruitingApplication.createMemberDraft(
            form,
            200L,
            1L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
    }

    private RecruitingRound configuredRound() {
        return RecruitingRound.createRegular(season(), RecruitingRoundConfiguration.of(
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
        ));
    }

    private RecruitingApplication submittedApplication() {
        RecruitingApplication application = draftApplication();
        application.submit(1L);
        return application;
    }
}
