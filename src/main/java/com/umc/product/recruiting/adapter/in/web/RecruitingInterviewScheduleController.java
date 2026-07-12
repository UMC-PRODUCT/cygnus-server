package com.umc.product.recruiting.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitRecruitingInterviewAvailabilityRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewScheduleResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/applications/{applicationId}/interview-schedule")
@Validated
@Tag(name = "Recruiting | 면접 일정", description = "지원자가 면접 가능 일정을 제출하고 본인 일정을 조회합니다.")
@RequiredArgsConstructor
public class RecruitingInterviewScheduleController {

    private final ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    private final GetRecruitingInterviewScheduleUseCase getScheduleUseCase;

    @PutMapping("/availability")
    @Operation(
        operationId = "RECRUITING-SCHEDULE-001",
        summary = "면접 가능 일정 제출",
        description = "CurrentMember 지원자가 Form 가능 일정 응답 ID를 저장합니다."
    )
    public void submitAvailability(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody SubmitRecruitingInterviewAvailabilityRequest request
    ) {
        manageScheduleUseCase.submitAvailability(request.toCommand(applicationId, actor.getMemberId()));
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
