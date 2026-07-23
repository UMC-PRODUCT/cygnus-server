package com.umc.product.schedule.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleInfo;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;

public record ScheduleGraphQlResponse(
    Long scheduleId,
    String name,
    String description,
    Set<ScheduleTag> tags,
    Long authorMemberId,
    Instant startsAt,
    Instant endsAt,
    boolean online,
    ScheduleBaseInfo.ScheduleLocationInfo location,
    boolean attendanceChecked,
    ScheduleBaseInfo.ScheduleAttendancePolicyInfo attendancePolicy,
    List<Long> participantMemberIds,
    Viewer viewer
) {

    public static ScheduleGraphQlResponse from(ScheduleInfo info) {
        ScheduleBaseInfo base = info.baseInfo();
        return new ScheduleGraphQlResponse(
            base.scheduleId(),
            base.name(),
            base.description(),
            base.tags(),
            base.authorMemberId(),
            base.startsAt(),
            base.endsAt(),
            base.isOnline(),
            base.location(),
            base.isAttendanceChecked(),
            base.attendancePolicy(),
            info.participants().stream().map(ScheduleInfo.ScheduleParticipantInfo::memberId).toList(),
            new Viewer(info.isParticipant(), info.attendanceStatus())
        );
    }

    public record Viewer(
        boolean participant,
        AttendanceStatus attendanceStatus
    ) {
    }
}
