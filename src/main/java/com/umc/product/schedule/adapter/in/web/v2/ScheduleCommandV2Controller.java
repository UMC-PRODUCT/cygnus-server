package com.umc.product.schedule.adapter.in.web.v2;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.umc.product.schedule.adapter.in.web.v2.dto.request.CreateScheduleRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.EditScheduleRequest;
import com.umc.product.schedule.application.port.in.command.CreateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.DeleteScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;

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
@Tag(name = "Schedule V2 | Command", description = "일정 생성, 수정, 삭제와 출석 요청을 다룹니다.")
public class ScheduleCommandV2Controller {

    private final CreateScheduleUseCase createScheduleUseCase;
    private final UpdateScheduleUseCase updateScheduleUseCase;
    private final DeleteScheduleUseCase deleteScheduleUseCase;

    @CheckAccess(
        resourceType = ResourceType.SCHEDULE,
        permission = PermissionType.WRITE,
        message = "일정을 만들려면 챌린저 활동 기록이 필요해요. 활동 기록을 확인해주세요."
    )
    @Operation(operationId = "SCHEDULE-C001", summary = "일정 생성", description = """
        일정을 생성합니다. `location` 필드를 작성하지 않으실 경우 비대면 일정으로 간주됩니다.

        스터디 일정을 생성하고자 하는 경우에는, 반드시 별도의 API를 이용해서 생성해야 합니다.
        그렇지 않은 경우, 스터디 미진행으로 인한 벌점이 부과될 수 있습니다.

        하루종일 일정의 경우, 클라이언트 단에서 KST 기준으로 시작일 00:00:000 ~ 종료일 23:59:999 을 UTC-Based ISO8601 형식으로 보내주셔야 합니다.

        조회할 때도 반대로 KST 기준 00:00:000에 시작해서 23:59:999에 끝나는 일정의 경우에 자동으로 하루 종일로 표시해주시면 됩니다.

        > *e.g.* 2025-12-08T15:00:00.000Z ~ 2025-12-09T14:59:59.999Z -> KST 기준 25/12/09 00:00:000~23:59:999, 하루종일로 표시!

        출석 요청 관련 일시 필드는 운영진만 입력 가능합니다.
        또한 일정 생성 시, 초대 가능한 챌린저 수에 제한이 존재합니다.

        1. 일반 챌린저: 50명
        2. 회장단: 100명
        3. 파트장/기타 운영진: 100명
        4. 지부장: 300명
        5. 중앙 운영진: 300명
        6. 총괄단: 2,000명

        이 부분에 대한 사용자별 권한 확인은 SCHEDULE-Q001 `/api/v2/schedules/capabilities` API를 사용해주세요.

        (생성 예정) 일정 생성 시 참여자 목록을 기반으로 해당 인원들이 참여하는 스터디 그룹이 있는지를
        검사하는 API를 토대로 사용자에게 경고 등을 띄울 것을 추천드립니다.

        생성된 일정의 ID 값을 반환합니다.
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = """
            SCHEDULE-0006 : 시작 시간은 종료 시간보다 빨라야 해요. 시간을 다시 선택해주세요.<br>
            SCHEDULE-0010 : 태그를 1개 이상 선택해주세요.<br>
            SCHEDULE-0025 : 현재 기수의 일정만 만들 수 있어요. 기수를 확인해주세요.<br>
            SCHEDULE-0030 : 초대 가능한 참여자 수를 초과했어요. 참여자를 줄여주세요.<br>
            SCHEDULE-0032 : 초대할 수 없는 참여자가 포함되어 있어요. 참여자 목록을 확인해주세요.<br>
            CHALLENGER-0009 : 일정을 만들려면 챌린저 상태가 활동 중이거나 수료여야 해요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "403", description = """
            AUTHORIZATION-0001 : 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.<br>
            SCHEDULE-0029 : 일정을 만들려면 챌린저 활동 이력이 필요해요. 활동 기록을 확인해주세요.<br>
            SCHEDULE-0031 : 출석이 필요한 일정을 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.
            """,
            content = @Content
        )
    })
    @PostMapping
    public Long create(
        @Valid @RequestBody CreateScheduleRequest request,
        @CurrentMember MemberPrincipal memberPrincipal) {

        CreateScheduleCommand command = request.toCommand(memberPrincipal.getMemberId());

        return createScheduleUseCase.create(command);
    }

    @CheckAccess(
        resourceType = ResourceType.SCHEDULE,
        resourceId = "#scheduleId",
        permission = PermissionType.EDIT,
        message = "일정은 생성자 본인 또는 해당 기수의 최고 운영 관리자만 수정할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Operation(operationId = "SCHEDULE-C002", summary = "일정 수정", description = """
        일정과 관련된 모든 정보를 수정합니다. 제공되지 않은 필드는 변경하지 않는 것으로 간주합니다.

        ---
        ### `isOnline` (대면/비대면 전환 플래그)
        일정의 대면 <-> 비대면 상태를 명시적으로 전환할 때 사용합니다.
        * **`null` (또는 생략)** : 기존 상태 유지
        > ⚠️ **주의:** 기존 대면 일정에서 **장소(location)만 다른 곳으로 변경**하는 경우, 속성이 전환되는 것이 아니므로 반드시 `null`로 보내주세요.
        * **`true`** : **비대면** 일정으로 전환 (기존 장소 데이터 삭제)
        * **`false`** : **대면** 일정으로 전환 (반드시 `location` 데이터를 함께 전송해야 함)
        ---
        ### `isAttendanceRequired` (출석 체크 여부 전환 플래그)
        일정의 출석 체크 상태를 명시적으로 전환할 때 사용합니다.
        * **`null` (또는 생략)** : 기존 출석 상태 유지
        * **`true`** : **출석 O** 일정으로 전환 (반드시 `attendancePolicy` 데이터를 함께 전송해야 함)
        * **`false`** : **출석 X** 일정으로 전환 (기존 출석 정책 데이터 삭제)
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = """
            SCHEDULE-0006 : 시작 시간은 종료 시간보다 빨라야 해요. 시간을 다시 선택해주세요.<br>
            SCHEDULE-0010 : 태그를 1개 이상 선택해주세요.<br>
            SCHEDULE-0020 : 대면 일정에는 위치 정보가 필요해요. 위치를 입력해주세요.<br>
            SCHEDULE-0024 : 비대면 일정에는 위치 정보를 포함할 수 없어요. 위치 정보를 제거해주세요.<br>
            SCHEDULE-0027 : 출석이 필요한 일정에는 출석 정책을 설정해주세요.<br>
            SCHEDULE-0028 : 이미 시작된 일정은 수정할 수 없어요. 일정 시간을 확인해주세요.<br>
            SCHEDULE-0030 : 초대 가능한 참여자 수를 초과했어요. 참여자를 줄여주세요.<br>
            SCHEDULE-0032 : 초대할 수 없는 참여자가 포함되어 있어요. 참여자 목록을 확인해주세요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "403", description = """
            AUTHORIZATION-0002 : 이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.<br>
            SCHEDULE-0031 : 출석이 필요한 일정을 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "404", description = """
            SCHEDULE-0009 : 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요.
            """,
            content = @Content
        )
    })
    @PatchMapping("/{scheduleId}")
    public Long edit(
        @PathVariable Long scheduleId,
        @Valid @RequestBody EditScheduleRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        EditScheduleCommand command = request.toCommand(scheduleId, memberPrincipal.getMemberId());

        return updateScheduleUseCase.update(command);
    }

    @CheckAccess(
        resourceType = ResourceType.SCHEDULE,
        resourceId = "#scheduleId",
        permission = PermissionType.DELETE,
        message = "일정은 생성자 본인 또는 해당 기수의 최고 운영 관리자만 삭제할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Operation(operationId = "SCHEDULE-C006", summary = "일정 삭제", description = """
        일정을 삭제합니다.

        - 일정에 연결된 모든 참여자(ScheduleParticipant) 정보도 함께 삭제됩니다.
        - 일정 참여자 중 출석 기록(`attendance.status`)이 존재하는 사용자가 한 명이라도 있는 경우 삭제할 수 없습니다.
          (이 경우, 강제 삭제 API를 사용해야 합니다.)

        삭제 가능 권한:
        - 일정 생성자 본인
        - 전역 시스템 관리자(`SUPER_ADMIN`)
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = """
            SCHEDULE-0033 : 출석 기록이 있는 일정은 삭제할 수 없어요. 출석 기록을 먼저 확인해주세요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "403", description = """
            AUTHORIZATION-0002 : 이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "404", description = """
            SCHEDULE-0009 : 일정을 찾을 수 없습니다.
            """,
            content = @Content
        )
    })
    @DeleteMapping("/{scheduleId}")
    public void delete(
        @PathVariable Long scheduleId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        deleteScheduleUseCase.delete(scheduleId, memberPrincipal.getMemberId());
    }

    @CheckAccess(
        resourceType = ResourceType.SCHEDULE,
        resourceId = "#scheduleId",
        permission = PermissionType.FORCE_DELETE,
        message = "일정 강제 삭제는 해당 기수의 최고 운영 관리자만 할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @Operation(operationId = "SCHEDULE-C007", summary = "일정 강제 삭제", description = """
        출석 기록 존재 여부와 관계 없이 일정을 강제로 삭제합니다.

        - 일정에 연결된 모든 참여자(ScheduleParticipant) 정보도 함께 삭제됩니다.
        - 출석 기록이 있는 일정 또한 삭제 가능합니다.

        강제 삭제 가능 권한:
        - 전역 시스템 관리자(`SUPER_ADMIN`)
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "403", description = """
            AUTHORIZATION-0002 : 이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.
            """,
            content = @Content
        ),
        @ApiResponse(responseCode = "404", description = """
            SCHEDULE-0009 : 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요.
            """,
            content = @Content
        )
    })
    @DeleteMapping("/{scheduleId}/force")
    public void forceDelete(
        @PathVariable Long scheduleId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        deleteScheduleUseCase.forceDelete(scheduleId, memberPrincipal.getMemberId());
    }

}
