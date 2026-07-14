package com.umc.product.schedule.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;

final class ScheduleAttendanceAuditFixtures {

    static final long SCHEDULE_ID = 100L;
    static final long DECIDER_ID = 99L;
    static final Instant STARTS_AT = Instant.now().plusSeconds(86_400);
    static final Instant ENDS_AT = STARTS_AT.plusSeconds(7_200);

    private ScheduleAttendanceAuditFixtures() {
    }

    static RecordAuditLogCommand command(List<RecordAuditLogCommand> commands, String targetId) {
        return commands.stream().filter(command -> command.targetId().equals(targetId)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> section(RecordAuditLogCommand command, String name) {
        return (Map<String, Object>) command.details().get(name);
    }

    static void assertParticipantSnapshot(Map<String, Object> snapshot, long participantId, String name) {
        assertThat(snapshot)
            .containsEntry("type", "Member")
            .containsEntry("id", participantId)
            .containsEntry("memberId", participantId)
            .containsEntry("name", name)
            .containsEntry("nickname", name)
            .containsEntry("schoolName", "테스트대학교")
            .containsEntry("status", "ACTIVE");
    }

    static void assertScheduleSnapshot(Map<String, Object> snapshot) {
        assertThat(snapshot)
            .containsEntry("type", "Schedule")
            .containsEntry("id", SCHEDULE_ID)
            .containsEntry("name", "정기 세션")
            .containsEntry("status", "UPCOMING")
            .containsEntry("period", STARTS_AT + "/" + ENDS_AT);
    }

    static Schedule attendanceSchedule() {
        Schedule schedule = Schedule.builder()
            .name("정기 세션")
            .description("자유 입력 일정 설명")
            .tags(Set.of(ScheduleTag.STUDY))
            .authorMemberId(10L)
            .startsAt(STARTS_AT)
            .endsAt(ENDS_AT)
            .policy(Schedule.createAttendancePolicy(
                STARTS_AT.minusSeconds(172_800),
                STARTS_AT.plusSeconds(600),
                STARTS_AT.plusSeconds(1_200),
                STARTS_AT,
                ENDS_AT
            ))
            .build();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        return schedule;
    }

    static ScheduleParticipant participant(Schedule schedule, long memberId, AttendanceStatus status) {
        ScheduleParticipantAttendance attendance = status == null
            ? null
            : ScheduleParticipantAttendance.create(null, false, null, status);
        return ScheduleParticipant.builder()
            .memberId(memberId)
            .schedule(schedule)
            .attendance(attendance)
            .build();
    }

    static DecideAttendanceCommand decision(long participantId, boolean approved, String reason) {
        return DecideAttendanceCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .decidedByMemberId(DECIDER_ID)
            .participantMemberId(participantId)
            .isApproved(approved)
            .reason(reason)
            .build();
    }

    static MemberInfo member(long id, String name) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .nickname(name)
            .email(name + "@test.com")
            .schoolName("테스트대학교")
            .status(MemberStatus.ACTIVE)
            .build();
    }
}
