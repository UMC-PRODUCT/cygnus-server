package com.umc.product.schedule.application.service.command;

import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.DECIDER_ID;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.SCHEDULE_ID;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.assertParticipantSnapshot;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.assertScheduleSnapshot;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.attendanceSchedule;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.command;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.decision;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.member;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.participant;
import static com.umc.product.schedule.application.service.command.ScheduleAttendanceAuditFixtures.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("일정 참여 및 출석 rich audit")
class ScheduleAttendanceAuditTest {

    @Mock
    LoadSchedulePort loadSchedulePort;
    @Mock
    SaveScheduleParticipantPort saveScheduleParticipantPort;
    @Mock
    LoadScheduleParticipantPort loadScheduleParticipantPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;
    ScheduleParticipantCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new ScheduleParticipantCommandService(
            loadSchedulePort,
            saveScheduleParticipantPort,
            loadScheduleParticipantPort,
            new ScheduleAttendanceAuditRecorder(getMemberUseCase, recordAuditLogUseCase)
        );
    }

    @Test
    @DisplayName("출석 체크는 참여자 회원과 일정 스냅샷 및 결과 상태를 발행한다")
    void 출석_체크는_회원과_일정_스냅샷을_발행한다() {
        // given
        long participantId = 21L;
        Schedule schedule = attendanceSchedule();
        ScheduleParticipant participant = participant(schedule, participantId, null);
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, participantId))
            .willReturn(Optional.of(participant));
        given(getMemberUseCase.findAllByIds(Set.of(participantId)))
            .willReturn(Map.of(participantId, member(participantId, "출석자")));
        given(saveScheduleParticipantPort.save(participant)).willReturn(participant);
        ScheduleAttendanceCommand command = ScheduleAttendanceCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .requesterMemberId(participantId)
            .locationVerified(true)
            .build();

        // when
        sut.createScheduleParticipantWithAttendance(command);

        // then
        RecordAuditLogCommand event = capturedCommands().getFirst();
        assertThat(event.action()).isEqualTo(AuditAction.CHECK);
        assertThat(event.targetType()).isEqualTo("ScheduleAttendance");
        assertThat(event.targetId()).isEqualTo("100:21");
        assertThat(event.actorMemberId()).isEqualTo(participantId);
        assertThat(event.source()).isEqualTo(AuditSource.EXPLICIT_RECORDER);
        assertParticipantSnapshot(section(event, "target"), participantId, "출석자");
        assertScheduleSnapshot(section(event, "before"));
        assertThat(section(event, "after")).containsEntry("status", "PRESENT_PENDING");
    }

    @Test
    @DisplayName("공결 신청은 자유 입력 사유와 위치 및 이메일을 감사 details에서 제외한다")
    void 공결_신청은_민감_자유_입력을_제외한다() {
        // given
        long participantId = 22L;
        String sensitiveReason = "병원 진료 token=secret 원문 사유";
        Schedule schedule = attendanceSchedule();
        ScheduleParticipant participant = participant(schedule, participantId, null);
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, participantId))
            .willReturn(Optional.of(participant));
        given(getMemberUseCase.findAllByIds(Set.of(participantId)))
            .willReturn(Map.of(participantId, member(participantId, "공결자")));
        ExcuseScheduleAttendanceCommand command = ExcuseScheduleAttendanceCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .requesterMemberId(participantId)
            .isVerified(false)
            .latitude(37.1234)
            .longitude(127.1234)
            .excuseReason(sensitiveReason)
            .build();

        // when
        sut.createExcusedScheduleParticipantWithAttendance(command);

        // then
        RecordAuditLogCommand event = capturedCommands().getFirst();
        assertThat(event.action()).isEqualTo(AuditAction.SUBMIT);
        assertParticipantSnapshot(section(event, "target"), participantId, "공결자");
        assertScheduleSnapshot(section(event, "before"));
        assertThat(section(event, "after")).containsEntry("status", "EXCUSED_PENDING");
        assertThat(event.details().toString())
            .doesNotContain(sensitiveReason, "secret", "token", "email", "@test.com", "latitude", "longitude");
    }

    @Test
    @DisplayName("bulk 승인과 반려는 최대 100개의 대상별 APPROVE REJECT 이벤트 배열로 발행한다")
    void bulk_승인과_반려는_대상별_이벤트로_발행한다() {
        // given
        Schedule schedule = attendanceSchedule();
        ScheduleParticipant approved = participant(schedule, 21L, AttendanceStatus.PRESENT_PENDING);
        ScheduleParticipant rejected = participant(schedule, 22L, AttendanceStatus.EXCUSED_PENDING);
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 21L))
            .willReturn(Optional.of(approved));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 22L))
            .willReturn(Optional.of(rejected));
        given(getMemberUseCase.batchGetByIds(Set.of(21L, 22L, DECIDER_ID))).willReturn(Map.of(
            21L, member(21L, "승인 대상"),
            22L, member(22L, "반려 대상"),
            DECIDER_ID, member(DECIDER_ID, "결정자")
        ));
        List<DecideAttendanceCommand> commands = List.of(
            decision(21L, true, "승인 사유 원문"),
            decision(22L, false, "반려 사유 원문")
        );

        // when
        sut.decideAttendances(commands);

        // then
        List<RecordAuditLogCommand> events = capturedCommands();
        assertThat(events).hasSize(2);
        RecordAuditLogCommand approve = command(events, "100:21");
        RecordAuditLogCommand reject = command(events, "100:22");
        assertThat(approve.action()).isEqualTo(AuditAction.APPROVE);
        assertThat(reject.action()).isEqualTo(AuditAction.REJECT);
        assertParticipantSnapshot(section(approve, "target"), 21L, "승인 대상");
        assertParticipantSnapshot(section(reject, "target"), 22L, "반려 대상");
        assertScheduleSnapshot(section(approve, "before"));
        assertScheduleSnapshot(section(reject, "before"));
        assertThat(section(approve, "after")).containsEntry("status", "PRESENT");
        assertThat(section(reject, "after")).containsEntry("status", "ABSENT");
        assertThat(events.toString()).doesNotContain("승인 사유 원문", "반려 사유 원문");
        then(getMemberUseCase).should().batchGetByIds(Set.of(21L, 22L, DECIDER_ID));
    }

    @Test
    @DisplayName("빈 승인 반려 bulk는 기존 계약대로 빈 결과를 반환한다")
    void 빈_bulk는_빈_결과를_반환한다() {
        assertThat(sut.decideAttendances(List.of())).isEmpty();

        then(recordAuditLogUseCase).should(never()).record(anyCommand());
    }

    @Test
    @DisplayName("101건 승인 반려 bulk도 크기 제한 오류 없이 기존 처리 흐름에 진입한다")
    void 백일건_초과_bulk도_처리_흐름에_진입한다() {
        List<DecideAttendanceCommand> commands = LongStream.rangeClosed(1, 101)
            .mapToObj(memberId -> decision(memberId, true, null))
            .toList();

        assertThatThrownBy(() -> sut.decideAttendances(commands))
            .isInstanceOf(ScheduleDomainException.class)
            .satisfies(exception -> assertThat(((ScheduleDomainException) exception).getBaseCode().getCode())
                .isEqualTo("SCHEDULE-0009"));

        then(loadSchedulePort).should().findById(SCHEDULE_ID);
        then(recordAuditLogUseCase).should(never()).record(anyCommand());
    }

    @Test
    @DisplayName("bulk 중간 대상 실패 시 대상별 감사 이벤트를 하나도 발행하지 않는다")
    void bulk_partial_failure는_감사_이벤트를_발행하지_않는다() {
        // given
        Schedule schedule = attendanceSchedule();
        ScheduleParticipant first = participant(schedule, 21L, AttendanceStatus.PRESENT_PENDING);
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 21L))
            .willReturn(Optional.of(first));
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, 999L))
            .willReturn(Optional.empty());
        List<DecideAttendanceCommand> commands = List.of(
            decision(21L, true, null),
            decision(999L, false, null)
        );

        // when & then
        assertThatThrownBy(() -> sut.decideAttendances(commands))
            .isInstanceOf(ScheduleDomainException.class);
        then(recordAuditLogUseCase).should(never()).record(anyCommand());
    }

    private List<RecordAuditLogCommand> capturedCommands() {
        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should(org.mockito.Mockito.atLeastOnce()).record(captor.capture());
        return captor.getAllValues();
    }

    private static RecordAuditLogCommand anyCommand() {
        return org.mockito.ArgumentMatchers.any(RecordAuditLogCommand.class);
    }

}
