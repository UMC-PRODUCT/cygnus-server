package com.umc.product.schedule.adapter.in.web.v2;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.DecideAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ExcuseScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleParticipantAttendanceInfoResponse;
import com.umc.product.schedule.application.port.in.command.CreateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.result.ScheduleParticipantAttendanceResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule V2 | Attendance Command", description = "일정 출석 요청과 승인을 다룹니다.")
public class ScheduleAttendanceCommandV2Controller {

    private final CreateScheduleParticipantUseCase createAttendanceUseCase;
    private final UpdateScheduleParticipantUseCase updateAttendanceUseCase;

    @CheckAccess(
        resourceType = ResourceType.ATTENDANCE,
        resourceId = "#scheduleId",
        permission = PermissionType.WRITE,
        message = "출석은 챌린저 활동 기록이 있고 일정에 참여하는 사용자만 요청할 수 있어요. 참여자 목록을 확인해주세요."
    )
    @Operation(operationId = "SCHEDULE-C003", summary = "출석 요청하기", description = """
        특정 일정에 대한 출석을 요청합니다. 이미 처리된 요청이거나 허용 시간이 아니면 실패합니다.
        """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "SCHEDULE-0011/0018/0019/0021/0022/0023", content = @Content),
        @ApiResponse(responseCode = "403", description = "AUTHORIZATION-0002", content = @Content),
        @ApiResponse(responseCode = "404", description = "SCHEDULE-0009", content = @Content)
    })
    @PostMapping("/{scheduleId}/attendances/request")
    public ScheduleParticipantAttendanceInfoResponse requestAttendance(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long scheduleId,
        @Valid @RequestBody ScheduleAttendanceRequest request
    ) {
        ScheduleAttendanceCommand command = request.toCommand(scheduleId, memberPrincipal.getMemberId());
        return ScheduleParticipantAttendanceInfoResponse.from(
            createAttendanceUseCase.createScheduleParticipantWithAttendance(command)
        );
    }

    @CheckAccess(
        resourceType = ResourceType.ATTENDANCE,
        resourceId = "#scheduleId",
        permission = PermissionType.WRITE,
        message = "출석 사유는 챌린저 활동 기록이 있고 일정에 참여하는 사용자만 제출할 수 있어요. 참여자 목록을 확인해주세요."
    )
    @Operation(operationId = "SCHEDULE-C004", summary = "출석 사유 제출", description = """
        위치 인증이 불가능하거나 결석 인정을 요청할 때 사유를 제출합니다.
        """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "SCHEDULE-0013/0016/0021/0022", content = @Content),
        @ApiResponse(responseCode = "403", description = "AUTHORIZATION-0002", content = @Content),
        @ApiResponse(responseCode = "404", description = "SCHEDULE-0009", content = @Content)
    })
    @PostMapping("/{scheduleId}/attendances/excuse")
    public ScheduleParticipantAttendanceInfoResponse excuseAttendance(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long scheduleId,
        @Valid @RequestBody ExcuseScheduleAttendanceRequest request
    ) {
        ExcuseScheduleAttendanceCommand command = request.toCommand(scheduleId, memberPrincipal.getMemberId());
        return ScheduleParticipantAttendanceInfoResponse.from(
            createAttendanceUseCase.createExcusedScheduleParticipantWithAttendance(command)
        );
    }

    @CheckAccess(
        resourceType = ResourceType.ATTENDANCE,
        resourceId = "#scheduleId",
        permission = PermissionType.APPROVE,
        message = "출석 요청은 해당 일정 기수의 운영진만 승인하거나 거절할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Operation(operationId = "SCHEDULE-C005", summary = "[운영진용] 출석 요청 승인/거절", description = """
        여러 출석 요청을 한 트랜잭션에서 승인하거나 거절합니다.
        """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "SCHEDULE-0012/0014/0015/0017/0021/0022", content = @Content),
        @ApiResponse(responseCode = "403", description = "AUTHORIZATION-0002", content = @Content),
        @ApiResponse(responseCode = "404", description = "SCHEDULE-0009", content = @Content)
    })
    @PostMapping("/{scheduleId}/attendances/decide")
    public List<ScheduleParticipantAttendanceInfoResponse> decideAttendances(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long scheduleId,
        @Valid @RequestBody List<DecideAttendanceRequest> requests
    ) {
        List<DecideAttendanceCommand> commands = requests.stream()
            .map(request -> request.toCommand(scheduleId, memberPrincipal.getMemberId()))
            .toList();
        List<ScheduleParticipantAttendanceResult> results = updateAttendanceUseCase.decideAttendances(commands);
        return results.stream()
            .map(ScheduleParticipantAttendanceInfoResponse::from)
            .toList();
    }
}
