package com.umc.product.schedule.application.service.command;

import static com.umc.product.support.fixture.ScheduleUnitFixture.attendance;
import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.result.ScheduleParticipantAttendanceResult;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleParticipantCommandService")
class ScheduleParticipantCommandServiceTest {

    private static final Long SCHEDULE_ID = 10L;
    private static final Long MEMBER_ID = 2L;

    @Mock LoadSchedulePort loadSchedulePort;
    @Mock SaveScheduleParticipantPort saveScheduleParticipantPort;
    @Mock LoadScheduleParticipantPort loadScheduleParticipantPort;
    @Mock GetMemberUseCase getMemberUseCase;

    ScheduleParticipantCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new ScheduleParticipantCommandService(
            loadSchedulePort, saveScheduleParticipantPort, loadScheduleParticipantPort, getMemberUseCase
        );
    }

    @Test
    @DisplayName("위치가 있는 최초 출석 요청을 저장하고 좌표·pending 상태를 반환한다")
    void creates_attendance_with_location() {
        Schedule schedule = activeSchedule();
        ScheduleParticipant participant = participant(schedule);
        prepare(schedule, participant);

        ScheduleParticipantAttendanceResult result = sut.createScheduleParticipantWithAttendance(
            ScheduleAttendanceCommand.builder()
                .scheduleId(SCHEDULE_ID).requesterMemberId(MEMBER_ID).locationVerified(true)
                .latitude(37.0).longitude(127.0).build()
        );

        assertThat(result.latitude()).isEqualTo(37.0);
        assertThat(result.longitude()).isEqualTo(127.0);
        assertThat(result.status()).isEqualTo(AttendanceStatus.PRESENT_PENDING);
        assertThat(result.isPendingDecision()).isTrue();
        assertThat(result.hasDecisionMakerMember()).isFalse();
        then(saveScheduleParticipantPort).should().save(participant);
    }

    @Test
    @DisplayName("위치 없는 최초 출석과 사유 출석은 null 좌표를 보존한다")
    void creates_attendance_without_location_and_excuse() {
        Schedule attendanceSchedule = activeSchedule();
        ScheduleParticipant attendanceParticipant = participant(attendanceSchedule);
        prepare(attendanceSchedule, attendanceParticipant);
        ScheduleParticipantAttendanceResult attendanceResult = sut.createScheduleParticipantWithAttendance(
            ScheduleAttendanceCommand.builder()
                .scheduleId(SCHEDULE_ID).requesterMemberId(MEMBER_ID).locationVerified(false).build()
        );
        assertThat(attendanceResult.latitude()).isNull();
        assertThat(attendanceResult.longitude()).isNull();

        Schedule excuseSchedule = activeSchedule();
        ScheduleParticipant excuseParticipant = participant(excuseSchedule);
        prepare(excuseSchedule, excuseParticipant);
        ScheduleParticipantAttendanceResult excuseResult = sut.createExcusedScheduleParticipantWithAttendance(
            ExcuseScheduleAttendanceCommand.builder()
                .scheduleId(SCHEDULE_ID).requesterMemberId(MEMBER_ID).isVerified(false)
                .excuseReason("병원").build()
        );
        assertThat(excuseResult.status()).isEqualTo(AttendanceStatus.EXCUSED_PENDING);
        assertThat(excuseResult.excuseReason()).isEqualTo("병원");
        assertThat(excuseResult.latitude()).isNull();
    }

    @Test
    @DisplayName("사유 출석의 좌표가 모두 있으면 point로 변환한다")
    void excuse_with_location() {
        Schedule schedule = activeSchedule();
        ScheduleParticipant participant = participant(schedule);
        prepare(schedule, participant);

        ScheduleParticipantAttendanceResult result = sut.createExcusedScheduleParticipantWithAttendance(
            ExcuseScheduleAttendanceCommand.builder()
                .scheduleId(SCHEDULE_ID).requesterMemberId(MEMBER_ID).isVerified(true)
                .latitude(36.0).longitude(128.0).excuseReason("교통").build()
        );

        assertThat(result.latitude()).isEqualTo(36.0);
        assertThat(result.longitude()).isEqualTo(128.0);
    }

    @Test
    @DisplayName("승인과 거절을 입력 순서대로 처리하고 결정자 정보를 매핑한다")
    void decides_attendances_in_input_order() {
        Schedule schedule = schedule();
        ScheduleParticipant approved = participant(schedule, attendance(AttendanceStatus.PRESENT_PENDING));
        ScheduleParticipant rejected = participant(schedule, attendance(AttendanceStatus.LATE_PENDING));
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 2L))
            .willReturn(Optional.of(approved));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 3L))
            .willReturn(Optional.of(rejected));
        given(getMemberUseCase.getById(99L)).willReturn(MemberInfo.builder()
            .id(99L).name("관리자").nickname("admin").schoolId(5L).schoolName("학교").build());
        given(getMemberUseCase.getById(100L)).willReturn(null);

        List<ScheduleParticipantAttendanceResult> results = sut.decideAttendances(List.of(
            decision(2L, 99L, true), decision(3L, 100L, false)
        ));

        assertThat(results).extracting(ScheduleParticipantAttendanceResult::status)
            .containsExactly(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT);
        assertThat(results.get(0).hasDecisionMakerMember()).isTrue();
        assertThat(results.get(0).decisionMakerMemberInfo().name()).isEqualTo("관리자");
        assertThat(results.get(1).hasDecisionMakerMember()).isFalse();
        assertThat(results.get(1).decisionMakerMemberInfo()).isNull();
        then(saveScheduleParticipantPort).should().save(approved);
        then(saveScheduleParticipantPort).should().save(rejected);
        assertThat(sut.decideAttendances(List.of())).isEmpty();
    }

    @Test
    @DisplayName("일정·정책·참여자 존재 조건을 각 command 경계에서 검증한다")
    void validates_lookup_preconditions() {
        ScheduleAttendanceCommand attendanceCommand = ScheduleAttendanceCommand.builder()
            .scheduleId(SCHEDULE_ID).requesterMemberId(MEMBER_ID).locationVerified(true).build();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());
        assertError(() -> sut.createScheduleParticipantWithAttendance(attendanceCommand),
            ScheduleErrorCode.SCHEDULE_NOT_FOUND);

        Schedule noPolicy = schedule();
        noPolicy.removeAttendancePolicy();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(noPolicy));
        assertError(() -> sut.createScheduleParticipantWithAttendance(attendanceCommand),
            ScheduleErrorCode.SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST);

        Schedule schedule = activeSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.empty());
        assertError(() -> sut.createScheduleParticipantWithAttendance(attendanceCommand),
            ScheduleErrorCode.PARTICIPANT_NOT_FOUND);
    }

    private Schedule activeSchedule() {
        Instant now = Instant.now();
        return schedule(
            now.plus(2, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS),
            Schedule.createAttendancePolicy(
                now.minus(8, ChronoUnit.MINUTES),
                now.plus(12, ChronoUnit.MINUTES),
                now.plus(22, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.HOURS)
            )
        );
    }

    private void prepare(Schedule schedule, ScheduleParticipant participant) {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.of(participant));
    }

    private DecideAttendanceCommand decision(Long participantId, Long deciderId, boolean approved) {
        return DecideAttendanceCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .participantMemberId(participantId)
            .decidedByMemberId(deciderId)
            .isApproved(approved)
            .reason(approved ? "승인" : "반려")
            .build();
    }

    private void assertError(Runnable action, ScheduleErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
