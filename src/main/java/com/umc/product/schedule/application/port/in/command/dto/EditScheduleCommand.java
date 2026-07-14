package com.umc.product.schedule.application.port.in.command.dto;

import java.time.Instant;
import java.util.Set;

import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.Builder;

@Builder
public record EditScheduleCommand(
    Long scheduleId,
    Long actorMemberId,
    String name,
    String description,
    Set<ScheduleTag> tags,
    Instant startsAt,
    Instant endsAt,
    LocationInfo location,
    Boolean isOnline,
    AttendancePolicyInfo attendancePolicy,
    Boolean isAttendanceRequired,
    Set<Long> participantMemberIds
) {

    @Builder
    public record AttendancePolicyInfo(
        Instant checkInStartAt,
        Instant onTimeEndAt,
        Instant lateEndAt
    ) {
    }

    @Builder
    public record LocationInfo(
        Double latitude,
        Double longitude,
        String locationName
    ) {
    }

    // 참여자가 변경이 요청되었는지 확인, 즉 request의 participantMemberIds가 null이 아닌지 확인
    // 실제로 변경되었는지는 service 단에서 판별합니다.
    public boolean isParticipantsUpdateRequested() {
        return participantMemberIds != null;
    }

    // 장소 관련 판단
    public boolean isChangingToOffline() {
        return Boolean.FALSE.equals(isOnline);
    }

    public boolean isChangingToOnline() {
        return Boolean.TRUE.equals(isOnline);
    }

    // 출석 정책 관련 판단
    public boolean isChangingToAttendanceRequired() {
        return Boolean.TRUE.equals(isAttendanceRequired);
    }

    public boolean isChangingToAttendanceNotRequired() {
        return Boolean.FALSE.equals(isAttendanceRequired);
    }

    // 검증
    public void validate() {
        // 대면으로 변경하면서 location이 없으면 에러
        if (isChangingToOffline() && location == null) {
            throw new ScheduleDomainException(ScheduleErrorCode.OFFLINE_SCHEDULE_REQUIRES_LOCATION);
        }

        // 비대면으로 변경하면서 location이 있으면 에러
        if (isChangingToOnline() && location != null) {
            throw new ScheduleDomainException(ScheduleErrorCode.ONLINE_SCHEDULE_SHOULD_NOT_HAVE_LOCATION);
        }

        // 출석 필요로 변경하는데 정책 데이터가 없으면 에러
        if (isChangingToAttendanceRequired() && attendancePolicy == null) {
            throw new ScheduleDomainException(ScheduleErrorCode.ATTENDANCE_POLICY_REQUIRED);
        }
    }
}
