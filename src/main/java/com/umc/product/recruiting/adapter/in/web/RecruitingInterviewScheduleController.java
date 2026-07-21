package com.umc.product.recruiting.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewScheduleResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/applications/{applicationId}/interview-schedule")
@Validated
@Tag(name = "Recruiting | 면접 일정", description = "면접 가능 일정 제출 기능 상태와 본인 일정을 조회합니다.")
@RequiredArgsConstructor
public class RecruitingInterviewScheduleController {

    private final ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    private final GetRecruitingInterviewScheduleUseCase getScheduleUseCase;

    @PutMapping("/availability")
    @Operation(
        operationId = "RECRUITING-SCHEDULE-001",
        summary = "면접 가능 일정 제출 (미구현)",
        description = "Form SCHEDULE 응답을 직접 제출하는 기능은 Form 엔진 연동 전까지 사용할 수 없습니다."
    )
    @ApiResponse(responseCode = "501", description = "RECRUITING-0419: 면접 가능 일정 응답 제출 기능 미구현")
    public void submitAvailability(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId
    ) {
        manageScheduleUseCase.submitAvailability(
            SubmitRecruitingInterviewAvailabilityCommand.of(applicationId, actor.getMemberId())
        );
    }

    @GetMapping
    @Operation(
        operationId = "RECRUITING-SCHEDULE-002",
        summary = "면접 일정 조회",
        description = "CurrentMember가 조회 가능한 지원서의 기본 면접 일정 정보를 조회합니다. 연락처와 메일 오류는 반환하지 않습니다."
    )
    public ResponseEntity<RecruitingInterviewScheduleResponse> getSchedule(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId
    ) {
        return ResponseEntity.of(getScheduleUseCase.findByApplicationId(applicationId, actor.getMemberId())
            .map(RecruitingInterviewScheduleResponse::from));
    }
}
