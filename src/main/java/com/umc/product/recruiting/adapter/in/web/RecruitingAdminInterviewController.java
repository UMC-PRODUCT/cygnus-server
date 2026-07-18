package com.umc.product.recruiting.adapter.in.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.ConfirmRecruitingInterviewScheduleRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RequestRecruitingInterviewScheduleRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Validated
@Tag(name = "Recruiting | 면접 일정 관리", description = "운영진이 면접 가능 일정 요청과 확정 정보를 저장합니다.")
@RequiredArgsConstructor
public class RecruitingAdminInterviewController {

    private final ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;

    @PostMapping("/applications/{applicationId}/interview-schedule/request")
    @Operation(
        operationId = "RECRUITING-ADMIN-SCHEDULE-001",
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
        operationId = "RECRUITING-ADMIN-SCHEDULE-002",
        summary = "면접 일정 확정",
        description = "CurrentMember 운영 권한으로 저장된 가능 일정 응답에 대한 면접 시간을 확정합니다. overlap 조회와 이메일 발송은 수행하지 않습니다."
    )
    public void confirm(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody ConfirmRecruitingInterviewScheduleRequest request
    ) {
        manageScheduleUseCase.confirm(request.toCommand(applicationId, memberPrincipal.getMemberId()));
    }
}
