package com.umc.product.schedule.domain;

import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_ENDS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_STARTS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.attendance;
import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.point;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@DisplayName("Schedule 도메인")
class ScheduleDomainTest {

    @Test
    @DisplayName("유효한 일정은 기본 정보와 출석 정책을 보존한다")
    void creates_valid_schedule() {
        Schedule schedule = schedule();

        assertThat(schedule.getName()).isEqualTo("정기 세션");
        assertThat(schedule.getTags()).containsExactly(ScheduleTag.GENERAL);
        assertThat(schedule.getPolicy().getEarlyCheckInMinutes()).isEqualTo(10L);
        assertThat(schedule.getPolicy().getAttendanceGraceMinutes()).isEqualTo(10L);
        assertThat(schedule.getPolicy().getLateToleranceMinutes()).isEqualTo(10L);
    }

    @Test
    @DisplayName("tag가 null 또는 비어 있으면 일정을 생성할 수 없다")
    void tag_is_required() {
        assertScheduleError(() -> scheduleBuilder().tags(null).build(), ScheduleErrorCode.TAG_REQUIRED);
        assertScheduleError(() -> scheduleBuilder().tags(Set.of()).build(), ScheduleErrorCode.TAG_REQUIRED);
    }

    @Test
    @DisplayName("종료가 시작보다 빠르거나 출석 인정 종료보다 빠르면 거부한다")
    void validates_time_range() {
        assertScheduleError(
            () -> schedule(DEFAULT_ENDS_AT, DEFAULT_STARTS_AT, null),
            ScheduleErrorCode.INVALID_TIME_RANGE
        );
        AttendancePolicy longPolicy = AttendancePolicy.create(10L, 90L, 60L);
        assertScheduleError(
            () -> schedule(DEFAULT_STARTS_AT, DEFAULT_STARTS_AT.plus(2, ChronoUnit.HOURS), longPolicy),
            ScheduleErrorCode.INVALID_TIME_RANGE
        );
    }

    @Test
    @DisplayName("출석 정책 시각은 check-in, 시작, 출석, 지각, 종료 순서여야 한다")
    void attendance_policy_time_order_is_strict() {
        Instant start = DEFAULT_STARTS_AT;
        assertScheduleError(() -> Schedule.createAttendancePolicy(
            start, start, start.plusSeconds(1), start, DEFAULT_ENDS_AT
        ), ScheduleErrorCode.INVALID_TIME_RANGE);
        assertScheduleError(() -> Schedule.createAttendancePolicy(
            start.minusSeconds(1), start, start.plusSeconds(1), start, DEFAULT_ENDS_AT
        ), ScheduleErrorCode.INVALID_TIME_RANGE);
        assertScheduleError(() -> Schedule.createAttendancePolicy(
            start.minusSeconds(1), start.plusSeconds(1), start.plusSeconds(1), start, DEFAULT_ENDS_AT
        ), ScheduleErrorCode.INVALID_TIME_RANGE);
        assertScheduleError(() -> Schedule.createAttendancePolicy(
            start.minusSeconds(1), start.plusSeconds(1), DEFAULT_ENDS_AT, start, DEFAULT_ENDS_AT
        ), ScheduleErrorCode.INVALID_TIME_RANGE);
    }

    @Test
    @DisplayName("진행·종료 판정은 시작과 종료 경계를 제외한다")
    void progress_boundaries_are_exclusive() {
        Schedule schedule = schedule(DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, null);

        assertThat(schedule.isInProgress(DEFAULT_STARTS_AT)).isFalse();
        assertThat(schedule.isInProgress(DEFAULT_STARTS_AT.plusSeconds(1))).isTrue();
        assertThat(schedule.isInProgress(DEFAULT_ENDS_AT)).isFalse();
        assertThat(schedule.isEnded(DEFAULT_ENDS_AT)).isFalse();
        assertThat(schedule.isEnded(DEFAULT_ENDS_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("현재 시각과 출석 정책에 따라 최초 출석 상태를 결정한다")
    void determines_attendance_status_at_time_boundaries() {
        Instant now = Instant.now();
        Schedule present = schedule(
            now.plus(2, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(10L, 10L, 10L)
        );
        Schedule late = schedule(
            now.minus(15, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(30L, 10L, 10L)
        );
        Schedule absent = schedule(
            now.minus(30, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(60L, 5L, 5L)
        );
        Schedule tooEarly = schedule(
            now.plus(30, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(1L, 5L, 5L)
        );

        assertThat(present.getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT_PENDING);
        assertThat(late.getAttendanceStatus()).isEqualTo(AttendanceStatus.LATE_PENDING);
        assertThat(absent.getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertScheduleError(tooEarly::getAttendanceStatus, ScheduleErrorCode.CHECK_IN_TOO_EARLY);
    }

    @Test
    @DisplayName("출석 정책이 없거나 이미 종료된 일정은 출석 상태를 계산하지 않는다")
    void attendance_status_requires_active_policy() {
        Instant now = Instant.now();
        Schedule noPolicy = schedule(now.minusSeconds(10), now.plusSeconds(10), null);
        Schedule ended = schedule(
            now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(10L, 10L, 10L)
        );

        assertScheduleError(noPolicy::getAttendanceStatus,
            ScheduleErrorCode.SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST);
        assertScheduleError(ended::getAttendanceStatus, ScheduleErrorCode.SCHEDULE_ENDED);
    }

    @Test
    @DisplayName("수정은 제공된 값만 반영하고 시간·정책 조합을 다시 검증한다")
    void partial_update_preserves_omitted_values() {
        Schedule schedule = schedule();
        Instant changedStart = DEFAULT_STARTS_AT.plus(1, ChronoUnit.HOURS);
        Instant changedEnd = DEFAULT_ENDS_AT.plus(2, ChronoUnit.HOURS);
        AttendancePolicy changedPolicy = AttendancePolicy.create(5L, 5L, 5L);

        schedule.update(
            "수정 일정", "수정 설명", Set.of(ScheduleTag.STUDY),
            changedStart, changedEnd, "새 장소", point(128.0, 36.0), changedPolicy
        );

        assertThat(schedule.getName()).isEqualTo("수정 일정");
        assertThat(schedule.getDescription()).isEqualTo("수정 설명");
        assertThat(schedule.getTags()).containsExactly(ScheduleTag.STUDY);
        assertThat(schedule.getStartsAt()).isEqualTo(changedStart);
        assertThat(schedule.getEndsAt()).isEqualTo(changedEnd);
        assertThat(schedule.getLocationName()).isEqualTo("새 장소");
        assertThat(schedule.getPolicy()).isSameAs(changedPolicy);
        schedule.update(null, null, null, null, null, null, null, null);
        assertThat(schedule.getName()).isEqualTo("수정 일정");
        assertScheduleError(
            () -> schedule.update(null, null, Set.of(), null, null, null, null, null),
            ScheduleErrorCode.TAG_REQUIRED
        );
        assertScheduleError(
            () -> schedule.update(null, null, null, changedEnd.plusSeconds(1), null, null, null, null),
            ScheduleErrorCode.INVALID_TIME_RANGE
        );
    }

    @Test
    @DisplayName("온라인 전환은 장소만 제거하고 출석 정책 제거는 독립적으로 동작한다")
    void online_and_policy_conversion_are_independent() {
        Schedule schedule = schedule();

        schedule.convertToOnline();

        assertThat(schedule.getLocation()).isNull();
        assertThat(schedule.getLocationName()).isNull();
        assertThat(schedule.getPolicy()).isNotNull();
        schedule.removeAttendancePolicy();
        assertThat(schedule.getPolicy()).isNull();
    }

    @Test
    @DisplayName("출석 요청은 정책과 최초 요청 조건을 검증한다")
    void participant_create_attendance_validates_preconditions() {
        Instant now = Instant.now();
        Schedule active = schedule(
            now.plus(2, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            AttendancePolicy.create(10L, 10L, 10L)
        );
        ScheduleParticipant participant = participant(active);

        participant.createAttendance(point(127.0, 37.0), null);

        assertThat(participant.getAttendance().getStatus()).isEqualTo(AttendanceStatus.PRESENT_PENDING);
        assertThat(participant.getAttendance().getLocationVerified()).isFalse();
        assertScheduleError(
            () -> participant.createAttendance(null, true),
            ScheduleErrorCode.NOT_FIRST_ATTENDANCE_REQUEST
        );
        assertScheduleError(
            () -> participant(schedule(DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, null)).createAttendance(null, true),
            ScheduleErrorCode.SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST
        );
    }

    @Test
    @DisplayName("사유 제출은 최초·결석·지각 상태를 각각 pending 상태로 전이한다")
    void submit_excuse_transitions_supported_states() {
        Schedule schedule = schedule();
        ScheduleParticipant first = participant(schedule);
        ScheduleParticipant absent = participant(schedule, attendance(AttendanceStatus.ABSENT));
        ScheduleParticipant late = participant(schedule, attendance(AttendanceStatus.LATE));

        first.submitExcuse(null, false, "병원 방문");
        absent.submitExcuse(null, true, "병원 방문");
        late.submitExcuse(null, true, "교통 지연");

        assertThat(first.getAttendance().getStatus()).isEqualTo(AttendanceStatus.EXCUSED_PENDING);
        assertThat(absent.getAttendance().getStatus()).isEqualTo(AttendanceStatus.ABSENT_EXCUSE_PENDING);
        assertThat(late.getAttendance().getStatus()).isEqualTo(AttendanceStatus.LATE_EXCUSE_PENDING);
        assertScheduleError(
            () -> participant(schedule).submitExcuse(null, false, ""),
            ScheduleErrorCode.NO_EXCUSE_REASON_GIVEN
        );
        assertScheduleError(
            () -> participant(schedule, attendance(AttendanceStatus.PRESENT)).submitExcuse(null, false, "사유"),
            ScheduleErrorCode.INVALID_ATTENDANCE_STATUS_FOR_EXCUSE
        );
    }

    @Test
    @DisplayName("승인·거절·강제 변경은 pending 상태를 올바른 최종 상태로 전이한다")
    void decides_pending_attendance() {
        assertApproved(AttendanceStatus.PRESENT_PENDING, AttendanceStatus.PRESENT);
        assertApproved(AttendanceStatus.LATE_PENDING, AttendanceStatus.LATE);
        assertApproved(AttendanceStatus.EXCUSED_PENDING, AttendanceStatus.EXCUSED);
        assertApproved(AttendanceStatus.ABSENT_EXCUSE_PENDING, AttendanceStatus.EXCUSED);
        assertApproved(AttendanceStatus.LATE_EXCUSE_PENDING, AttendanceStatus.EXCUSED);
        assertRejected(AttendanceStatus.PRESENT_PENDING, AttendanceStatus.ABSENT);
        assertRejected(AttendanceStatus.LATE_PENDING, AttendanceStatus.ABSENT);
        assertRejected(AttendanceStatus.EXCUSED_PENDING, AttendanceStatus.ABSENT);
        assertRejected(AttendanceStatus.ABSENT_EXCUSE_PENDING, AttendanceStatus.ABSENT);
        assertRejected(AttendanceStatus.LATE_EXCUSE_PENDING, AttendanceStatus.LATE);

        ScheduleParticipant forced = participant(schedule(), attendance(AttendanceStatus.EXCUSED_PENDING));
        forced.forceChangeAttendance(99L, AttendanceStatus.PRESENT);
        assertThat(forced.getAttendance().getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(forced.getAttendance().getDecidedByMemberId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("출석 기록이 없거나 확정 상태면 운영진 결정을 거부한다")
    void decision_requires_pending_attendance() {
        assertScheduleError(
            () -> participant(schedule()).approveAttendance(1L, "승인"),
            ScheduleErrorCode.NO_ATTENDANCE_RECORD
        );
        assertScheduleError(
            () -> participant(schedule(), attendance(AttendanceStatus.PRESENT)).rejectAttendance(1L, "반려"),
            ScheduleErrorCode.ATTENDANCE_NOT_REQUIRES_CONFIRM
        );
        assertScheduleError(
            () -> attendance(AttendanceStatus.PRESENT).approve(1L, "승인"),
            ScheduleErrorCode.INVALID_ATTENDANCE_STATUS_FOR_APPROVAL
        );
        assertScheduleError(
            () -> attendance(AttendanceStatus.PRESENT).reject(1L, "반려"),
            ScheduleErrorCode.INVALID_ATTENDANCE_STATUS_FOR_REJECT
        );
    }

    @Test
    @DisplayName("enum과 도메인 예외의 외부 오류 계약을 보존한다")
    void enum_and_error_contract() {
        assertThat(ScheduleTag.values()).allSatisfy(tag -> assertThat(tag.getDescription()).isNotBlank());
        assertThat(AttendanceStatus.values()).anySatisfy(status -> assertThat(status.isPending()).isTrue());
        assertThat(ScheduleErrorCode.values()).allSatisfy(code -> {
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getCode()).startsWith("SCHEDULE-");
            assertThat(code.getMessage()).isNotBlank();
            assertThat(new ScheduleDomainException(code).getBaseCode()).isEqualTo(code);
            assertThat(new ScheduleDomainException(code, "상세").getMessage()).contains("상세");
        });
    }

    private void assertApproved(AttendanceStatus before, AttendanceStatus after) {
        ScheduleParticipant participant = participant(schedule(), attendance(before));
        participant.approveAttendance(99L, "승인");
        assertThat(participant.getAttendance().getStatus()).isEqualTo(after);
        assertThat(participant.getAttendance().getDecidedAt()).isNotNull();
        assertThat(participant.getAttendance().getDecisionReason()).isEqualTo("승인");
    }

    private void assertRejected(AttendanceStatus before, AttendanceStatus after) {
        ScheduleParticipant participant = participant(schedule(), attendance(before));
        participant.rejectAttendance(99L, "반려");
        assertThat(participant.getAttendance().getStatus()).isEqualTo(after);
        assertThat(participant.getAttendance().getDecisionReason()).isEqualTo("반려");
    }

    private Schedule.ScheduleBuilder scheduleBuilder() {
        return Schedule.builder()
            .name("일정")
            .description("설명")
            .tags(Set.of(ScheduleTag.GENERAL))
            .authorMemberId(1L)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .policy(null);
    }

    private void assertScheduleError(Runnable action, ScheduleErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
