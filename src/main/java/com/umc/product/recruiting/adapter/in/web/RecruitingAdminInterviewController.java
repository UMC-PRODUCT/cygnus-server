package com.umc.product.recruiting.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.ConfirmRecruitingInterviewScheduleRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.ConfirmRecruitingInterviewSchedulesRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingInterviewSessionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RequestRecruitingInterviewScheduleRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewScheduleBoardResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewSessionResponse;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Validated
@Tag(name = "Recruiting | 면접 일정 관리", description = "운영진이 면접 가능 일정 요청과 확정 정보를 저장합니다.")
@RequiredArgsConstructor
public class RecruitingAdminInterviewController {

    private final ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    private final ManageRecruitingInterviewSessionUseCase manageSessionUseCase;
    private final GetRecruitingInterviewSessionUseCase getSessionUseCase;
    private final GetRecruitingInterviewScheduleBoardUseCase getScheduleBoardUseCase;
    private final ConfirmRecruitingInterviewSchedulesUseCase confirmSchedulesUseCase;

    @GetMapping("/rounds/{roundId}/interview-sessions")
    @Operation(
        operationId = "RECRUITING-ADMIN-053",
        summary = "Round 면접 세션 목록 조회",
        description = "CurrentMember 운영 권한으로 Round에 속한 면접 세션을 시작 시각 순서로 조회합니다."
    )
    public List<RecruitingInterviewSessionResponse> listSessions(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId
    ) {
        return getSessionUseCase.listSessions(roundId, memberPrincipal.getMemberId()).stream()
            .map(RecruitingInterviewSessionResponse::from)
            .toList();
    }

    @PostMapping("/rounds/{roundId}/interview-sessions")
    @Operation(
        operationId = "RECRUITING-ADMIN-054",
        summary = "Round 면접 세션 생성",
        description = "CurrentMember 운영 권한으로 Round 면접 기간 안의 15분 배수 슬롯 길이를 가진 면접 세션을 생성합니다."
    )
    public RecruitingIdResponse createSession(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody RecruitingInterviewSessionRequest request
    ) {
        return RecruitingIdResponse.from(manageSessionUseCase.createSession(
            request.toCreateCommand(roundId, memberPrincipal.getMemberId())
        ));
    }

    @PutMapping("/rounds/{roundId}/interview-sessions/{sessionId}")
    @Operation(
        operationId = "RECRUITING-ADMIN-055",
        summary = "Round 면접 세션 수정",
        description = "CurrentMember 운영 권한으로 어떤 지원자도 세션 슬롯에 최종 확정되지 않은 Round 면접 세션을 수정합니다."
    )
    public void updateSession(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long sessionId,
        @Valid @RequestBody RecruitingInterviewSessionRequest request
    ) {
        manageSessionUseCase.updateSession(request.toUpdateCommand(sessionId, roundId, memberPrincipal.getMemberId()));
    }

    @DeleteMapping("/rounds/{roundId}/interview-sessions/{sessionId}")
    @Operation(
        operationId = "RECRUITING-ADMIN-056",
        summary = "Round 면접 세션 삭제",
        description = "CurrentMember 운영 권한으로 어떤 지원자도 세션 슬롯에 최종 확정되지 않은 Round 면접 세션을 삭제합니다."
    )
    public void deleteSession(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long sessionId
    ) {
        manageSessionUseCase.deleteSession(DeleteRecruitingInterviewSessionCommand.of(
            sessionId, roundId, memberPrincipal.getMemberId()
        ));
    }

    @GetMapping("/rounds/{roundId}/interview-schedule-board")
    @Operation(
        operationId = "RECRUITING-ADMIN-057",
        summary = "KST 날짜 기준 면접 일정 보드 조회",
        description = "CurrentMember 운영 권한으로 KST 날짜에 속한 서버 계산 면접 슬롯과 지원자 배정 정보를 조회합니다."
    )
    public RecruitingInterviewScheduleBoardResponse getBoard(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @org.springframework.web.bind.annotation.RequestParam LocalDate date
    ) {
        return RecruitingInterviewScheduleBoardResponse.from(
            getScheduleBoardUseCase.getBoard(roundId, date, memberPrincipal.getMemberId())
        );
    }

    @PostMapping("/rounds/{roundId}/interview-schedule/confirmations")
    @Operation(
        operationId = "RECRUITING-ADMIN-058",
        summary = "Round 면접 일정 일괄 확정",
        description = "CurrentMember 운영 권한으로 세션 슬롯 기반 면접 일정을 원자적으로 일괄 확정합니다."
    )
    public void confirmAll(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody ConfirmRecruitingInterviewSchedulesRequest request
    ) {
        confirmSchedulesUseCase.confirmAll(request.toCommand(roundId, memberPrincipal.getMemberId()));
    }

    @PostMapping("/applications/{applicationId}/interview-schedule/request")
    @Operation(
        operationId = "RECRUITING-ADMIN-051",
        summary = "면접 가능 일정 요청 재시도",
        description = "자동 일정 요청이 없으면 생성하고, 메일 발송 실패 상태이면 Outbox 재시도를 요청합니다. 이미 처리 중이거나 발송된 요청은 기존 일정 ID를 반환합니다."
    )
    public RecruitingIdResponse requestAvailability(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody RequestRecruitingInterviewScheduleRequest request
    ) {
        return RecruitingIdResponse.from(manageScheduleUseCase.requestAvailability(
            request.toCommand(applicationId, memberPrincipal.getMemberId())
        ));
    }

    @PutMapping("/applications/{applicationId}/interview-schedule/confirmation")
    @Operation(
        operationId = "RECRUITING-ADMIN-052",
        summary = "면접 일정 확정",
        description = "CurrentMember 운영 권한으로 세션 슬롯 기반 단건 면접 일정을 확정합니다. 입력한 종료 시각과 장소는 세션 계산 결과와 일치해야 합니다."
    )
    public void confirm(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody ConfirmRecruitingInterviewScheduleRequest request
    ) {
        manageScheduleUseCase.confirm(request.toCommand(applicationId, memberPrincipal.getMemberId()));
    }
}
