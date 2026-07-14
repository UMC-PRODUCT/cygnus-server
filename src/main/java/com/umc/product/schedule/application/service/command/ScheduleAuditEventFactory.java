package com.umc.product.schedule.application.service.command;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;

final class ScheduleAuditEventFactory {

    private ScheduleAuditEventFactory() {
    }

    static ScheduleSnapshot snapshot(Schedule schedule) {
        return snapshot(schedule, null);
    }

    static ScheduleSnapshot snapshot(Schedule schedule, Integer participantCount) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "Schedule");
        values.put("id", schedule.getId());
        values.put("name", schedule.getName());
        values.put("status", scheduleStatus(schedule, Instant.now()));
        values.put("period", schedule.getStartsAt() + "/" + schedule.getEndsAt());
        putIfPresent(values, "participantCount", participantCount);
        return new ScheduleSnapshot(values);
    }

    static RecordAuditLogCommand scheduleCreated(
        Schedule schedule,
        MemberInfo actor,
        int participantCount
    ) {
        ScheduleSnapshot created = snapshot(schedule, participantCount);
        return scheduleCommand(
            AuditAction.CREATE,
            created,
            ScheduleSnapshot.empty(),
            created,
            schedule.getAuthorMemberId(),
            actor,
            "일정을 생성했습니다."
        );
    }

    static RecordAuditLogCommand scheduleUpdated(
        ScheduleSnapshot before,
        Schedule schedule,
        Long actorMemberId,
        MemberInfo actor
    ) {
        ScheduleSnapshot after = snapshot(schedule);
        return scheduleCommand(
            AuditAction.UPDATE,
            after,
            before,
            after,
            actorMemberId,
            actor,
            "일정을 수정했습니다."
        );
    }

    static RecordAuditLogCommand scheduleDeleted(
        ScheduleSnapshot before,
        Long actorMemberId,
        MemberInfo actor,
        boolean forced
    ) {
        return scheduleCommand(
            AuditAction.DELETE,
            before,
            before,
            ScheduleSnapshot.empty(),
            actorMemberId,
            actor,
            forced ? "일정을 강제로 삭제했습니다." : "일정을 삭제했습니다."
        );
    }

    static RecordAuditLogCommand participantChanged(
        ScheduleSnapshot schedule,
        Long actorMemberId,
        MemberInfo actor,
        Long participantMemberId,
        MemberInfo participant,
        AuditAction action
    ) {
        String status = action == AuditAction.CREATE ? "INVITED" : "REMOVED";
        return command(
            action,
            "ScheduleParticipant",
            schedule.id() + ":" + participantMemberId,
            actorMemberId,
            action == AuditAction.CREATE ? "일정 참여자를 추가했습니다." : "일정 참여자를 삭제했습니다.",
            memberValues(actorMemberId, actor),
            memberValues(participantMemberId, participant),
            context(schedule.id()),
            schedule.values(),
            Map.of("type", "ScheduleParticipant", "status", status)
        );
    }

    static RecordAuditLogCommand attendanceChanged(
        ScheduleSnapshot schedule,
        Long actorMemberId,
        MemberInfo actor,
        Long participantMemberId,
        MemberInfo participant,
        AttendanceStatus attendanceStatus,
        AuditAction action
    ) {
        String description = switch (action) {
            case CHECK -> "일정 출석을 요청했습니다.";
            case SUBMIT -> "일정 공결을 신청했습니다.";
            case APPROVE -> "일정 출석 요청을 승인했습니다.";
            case REJECT -> "일정 출석 요청을 반려했습니다.";
            default -> throw new IllegalArgumentException("지원하지 않는 일정 출석 감사 액션입니다: " + action);
        };
        return command(
            action,
            "ScheduleAttendance",
            schedule.id() + ":" + participantMemberId,
            actorMemberId,
            description,
            memberValues(actorMemberId, actor),
            memberValues(participantMemberId, participant),
            context(schedule.id()),
            schedule.values(),
            Map.of("type", "ScheduleAttendance", "status", attendanceStatus.name())
        );
    }

    private static RecordAuditLogCommand scheduleCommand(
        AuditAction action,
        ScheduleSnapshot target,
        ScheduleSnapshot before,
        ScheduleSnapshot after,
        Long actorMemberId,
        MemberInfo actor,
        String description
    ) {
        return command(
            action,
            "Schedule",
            String.valueOf(target.id()),
            actorMemberId,
            description,
            memberValues(actorMemberId, actor),
            target.values(),
            context(target.id()),
            before.values(),
            after.values()
        );
    }

    private static RecordAuditLogCommand command(
        AuditAction action,
        String targetType,
        String targetId,
        Long actorMemberId,
        String description,
        Map<String, Object> actor,
        Map<String, Object> target,
        Map<String, Object> context,
        Map<String, Object> before,
        Map<String, Object> after
    ) {
        return RecordAuditLogCommand.success(
            Domain.SCHEDULE,
            action,
            targetType,
            targetId,
            actorMemberId,
            description,
            RecordAuditLogCommand.structuredDetails(actor, target, context, before, after)
        );
    }

    private static Map<String, Object> memberValues(Long memberId, MemberInfo member) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "Member");
        values.put("id", memberId);
        values.put("memberId", memberId);
        if (member != null) {
            putIfPresent(values, "name", member.name());
            putIfPresent(values, "nickname", member.nickname());
            putIfPresent(values, "schoolName", member.schoolName());
            putIfPresent(values, "status", member.status() == null ? null : member.status().name());
        }
        return values;
    }

    private static Map<String, Object> context(Long scheduleId) {
        return Map.of(
            "outcome", "SUCCESS",
            "source", "EXPLICIT_RECORDER",
            "resourceType", "Schedule",
            "resourceId", scheduleId
        );
    }

    private static String scheduleStatus(Schedule schedule, Instant referenceTime) {
        if (referenceTime.isBefore(schedule.getStartsAt())) {
            return "UPCOMING";
        }
        if (referenceTime.isAfter(schedule.getEndsAt())) {
            return "COMPLETED";
        }
        return "IN_PROGRESS";
    }

    private static void putIfPresent(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    record ScheduleSnapshot(Map<String, Object> values) {

        static ScheduleSnapshot empty() {
            return new ScheduleSnapshot(Map.of());
        }

        Long id() {
            return (Long) values.get("id");
        }
    }
}
