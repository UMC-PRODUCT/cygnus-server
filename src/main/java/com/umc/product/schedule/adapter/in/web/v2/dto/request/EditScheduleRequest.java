package com.umc.product.schedule.adapter.in.web.v2.dto.request;

import java.time.Instant;
import java.util.Set;

import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.domain.enums.ScheduleTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * 일정에 관련된 사항을 변경할 때 사용하는 DTO 입니다.
 * <p>
 * 제공되지 않은 필드는 변경하지 않는 것으로 간주합니다.
 * <p>
 * 필드에 대한 자세한 사항은 {@link CreateScheduleRequest}을 참고해주시면 됩니다.
 */
public record EditScheduleRequest(
    @Schema(description = "일정 제목", example = "10기 OT", maxLength = 100)
    @Size(max = 100, message = "일정 제목은 최대 100자까지 입력 가능합니다") String name,

    @Schema(description = "메모/설명")
    String description,

    @Schema(description = "태그 목록 (null이면 기존 태그 유지)", example = "[\"STUDY\", \"GENERAL\"]")
    @Size(min = 1, message = "태그를 수정하려면 최소 1개 이상 선택해야 합니다") Set<ScheduleTag> tags,

    @Schema(description = "시작 일시 (UTC ISO8601. 예: 2026-05-21T01:00:00Z)", example = "2026-05-21T01:00:00Z")
    Instant startsAt,

    @Schema(description = "종료 일시 (UTC ISO8601. 예: 2026-08-20T03:00:00Z)", example = "2026-08-20T03:00:00Z")
    Instant endsAt,
    // 하루종일 일정은 따로 서버측에서 저장하지 않고,
    // 클라이언트 단에서 KST 기준 Instant로 알아서 변환하도록 합니다.

    @Valid ScheduleLocationRequest location,

    // patch 요청에서 대면 일정의 위치를 유지하거나, 대면 일정을 비대면 일정을 바꾸는 경우를 고려햐여, 명시적 플래그 필드를 추가합니다.
    @Schema(description = "null: 유지, true: 비대면으로 변경, false: 대면으로 변경")
    Boolean isOnline,

    @Valid ScheduleAttendancePolicyRequest attendancePolicy,

    @Schema(description = "null: 유지, true: 출석 필요로 변경, false: 출석 불필요로 변경")
    Boolean isAttendanceRequired, // 명시적 플래그 추가

    // 참여자는 중복되지 않도록 Set으로 받습니다.
    // 참여자 목록에는 반드시 요청한 사용자가 포함되어 있어야 하며,
    // 서버 측 생성자에서 add()를 통해서 강제로 참여시켜야 합니다.
    // (기획단 변경이 있기 전까지는 해당 사항을 유지합니다.)
    @Schema(description = "참여자 Member ID 목록", example = "[1, 2, 4]")
    Set<Long> participantMemberIds
) {

    public EditScheduleCommand toCommand(Long scheduleId, Long authorMemberId) {

        // Command의 locationInfo 생성
        EditScheduleCommand.LocationInfo locationInfo = this.location != null
            ? EditScheduleCommand.LocationInfo.builder()
            .latitude(this.location.latitude())
            .longitude(this.location.longitude())
            .locationName(this.location.locationName())
            .build() : null;

        EditScheduleCommand.AttendancePolicyInfo policyInfo = this.attendancePolicy != null
            ? EditScheduleCommand.AttendancePolicyInfo.builder()
            .checkInStartAt(this.attendancePolicy.checkInStartAt())
            .onTimeEndAt(this.attendancePolicy.onTimeEndAt())
            .lateEndAt(this.attendancePolicy.lateEndAt())
            .build() : null;

        return EditScheduleCommand.builder()
            .scheduleId(scheduleId)
            .actorMemberId(authorMemberId)
            .name(name)
            .description(description)
            .tags(tags)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .location(locationInfo)
            .isOnline(isOnline)
            .attendancePolicy(policyInfo)
            .isAttendanceRequired(isAttendanceRequired)
            .participantMemberIds(participantMemberIds)
            .build();
    }
}
