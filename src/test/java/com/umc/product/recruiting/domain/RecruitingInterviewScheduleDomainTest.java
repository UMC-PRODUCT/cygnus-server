package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting 면접 일정 도메인")
class RecruitingInterviewScheduleDomainTest {

    @Test
    @DisplayName("면접 가능 시간 요청은 메일 대기 상태로 생성된다")
    void 면접_가능_시간_요청은_메일_대기_상태로_생성된다() {
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            application(),
            "카카오톡 @umc"
        );

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        assertThat(schedule.getRequestMailStatus()).isEqualTo(RecruitingMailDeliveryStatus.PENDING);
        assertThat(schedule.getConfirmationMailStatus()).isEqualTo(RecruitingMailDeliveryStatus.PENDING);
        assertThat(schedule.getContactSnapshot()).isEqualTo("카카오톡 @umc");
    }

    @Test
    @DisplayName("가능 시간 응답 후 면접 일정을 확정한다")
    void 가능_시간_응답_후_면접_일정을_확정한다() {
        RecruitingInterviewSchedule schedule = schedule();
        Instant startsAt = Instant.parse("2026-08-12T01:00:00Z");
        Instant endsAt = Instant.parse("2026-08-12T01:30:00Z");

        schedule.submitAvailability(700L);
        schedule.confirm(startsAt, endsAt, "온라인", "카카오톡 @umc");

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.CONFIRMED);
        assertThat(schedule.getAvailabilityFormResponseId()).isEqualTo(700L);
        assertThat(schedule.getStartsAt()).isEqualTo(startsAt);
        assertThat(schedule.getEndsAt()).isEqualTo(endsAt);
        assertThat(schedule.getLocation()).isEqualTo("온라인");
    }

    @Test
    @DisplayName("가능 시간 응답 전에는 면접 일정을 확정할 수 없다")
    void 가능_시간_응답_전에는_면접_일정을_확정할_수_없다() {
        RecruitingInterviewSchedule schedule = schedule();

        assertThatThrownBy(() -> schedule.confirm(
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "카카오톡 @umc"
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("면접 종료 시각은 시작 시각보다 늦어야 한다")
    void 면접_종료_시각은_시작_시각보다_늦어야_한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);

        assertThatThrownBy(() -> schedule.confirm(
            Instant.parse("2026-08-12T01:30:00Z"),
            Instant.parse("2026-08-12T01:00:00Z"),
            "온라인",
            "카카오톡 @umc"
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD);
    }

    @Test
    @DisplayName("메일 실패와 성공은 시도 횟수와 결과를 기록한다")
    void 메일_실패와_성공은_시도_횟수와_결과를_기록한다() {
        RecruitingInterviewSchedule schedule = schedule();
        Instant sentAt = Instant.parse("2026-08-01T01:00:00Z");

        schedule.markRequestMailFailed("일시 오류");
        schedule.markRequestMailSent(sentAt);

        assertThat(schedule.getRequestMailAttempts()).isEqualTo(2);
        assertThat(schedule.getRequestMailStatus()).isEqualTo(RecruitingMailDeliveryStatus.SENT);
        assertThat(schedule.getRequestMailError()).isNull();
        assertThat(schedule.getRequestMailSentAt()).isEqualTo(sentAt);
    }

    private RecruitingInterviewSchedule schedule() {
        return RecruitingInterviewSchedule.requestAvailability(application(), "카카오톡 @umc");
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
                true,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-08-16T00:00:00Z"),
                300L,
                null,
                "문의 채널"
            )
        );
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 100L);
        return RecruitingApplication.createMemberDraft(
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
    }
}
