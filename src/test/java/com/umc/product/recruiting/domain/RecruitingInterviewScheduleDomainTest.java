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
        schedule.confirm(800L, startsAt, endsAt, "온라인", "카카오톡 @umc");

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.CONFIRMED);
        assertThat(schedule.getAvailabilityFormResponseId()).isEqualTo(700L);
        assertThat(schedule.getStartsAt()).isEqualTo(startsAt);
        assertThat(schedule.getEndsAt()).isEqualTo(endsAt);
        assertThat(schedule.getLocation()).isEqualTo("온라인");
    }

    @Test
    @DisplayName("면접 세션에 연결해 일정을 확정한다")
    void 면접_세션에_연결해_일정을_확정한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);

        schedule.confirm(
            800L,
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "카카오톡 @umc"
        );

        assertThat(schedule.getInterviewSessionId()).isEqualTo(800L);
    }

    @Test
    @DisplayName("가능 시간 응답 전에는 면접 일정을 확정할 수 없다")
    void 가능_시간_응답_전에는_면접_일정을_확정할_수_없다() {
        RecruitingInterviewSchedule schedule = schedule();

        assertThatThrownBy(() -> schedule.confirm(
            800L,
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
    @DisplayName("세션 ID 없이는 면접 일정을 확정할 수 없다")
    void 세션_ID_없이는_면접_일정을_확정할_수_없다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);

        assertThatThrownBy(() -> schedule.confirm(
            null,
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "카카오톡 @umc"
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);

        assertThatThrownBy(() -> schedule.confirm(
            0L,
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "카카오톡 @umc"
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);
    }

    @Test
    @DisplayName("가능 시간 응답은 요청 상태에서 한 번만 제출할 수 있다")
    void 가능_시간_응답은_요청_상태에서_한_번만_제출할_수_있다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);

        assertThatThrownBy(() -> schedule.submitAvailability(701L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);

        assertThat(schedule.getAvailabilityFormResponseId()).isEqualTo(700L);
    }

    @Test
    @DisplayName("취소된 일정에는 가능 시간 응답을 제출할 수 없다")
    void 취소된_일정에는_가능_시간_응답을_제출할_수_없다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.cancel();

        assertThatThrownBy(() -> schedule.submitAvailability(700L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);

        assertThat(schedule.getAvailabilityFormResponseId()).isNull();
    }

    @Test
    @DisplayName("가능 시간 Form 응답 ID는 양수여야 한다")
    void 가능_시간_Form_응답_ID는_양수여야_한다() {
        RecruitingInterviewSchedule schedule = schedule();

        assertThatThrownBy(() -> schedule.submitAvailability(0L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);

        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        assertThat(schedule.getAvailabilityFormResponseId()).isNull();
    }

    @Test
    @DisplayName("면접 종료 시각은 시작 시각보다 늦어야 한다")
    void 면접_종료_시각은_시작_시각보다_늦어야_한다() {
        RecruitingInterviewSchedule schedule = schedule();
        schedule.submitAvailability(700L);

        assertThatThrownBy(() -> schedule.confirm(
            800L,
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
                301L,
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
