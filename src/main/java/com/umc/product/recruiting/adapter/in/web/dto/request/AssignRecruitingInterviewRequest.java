package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "면접 배정 요청")
public record AssignRecruitingInterviewRequest(
    @Schema(description = "면접관 회원 ID", example = "1001")
    @NotNull Long interviewerMemberId,
    @Schema(description = "면접 시작 일시", example = "2026-07-10T10:00:00Z")
    @NotNull Instant startsAt,
    @Schema(description = "면접 종료 일시", example = "2026-07-10T10:30:00Z")
    @NotNull Instant endsAt,
    @Schema(description = "면접 장소 또는 접속 링크", example = "Zoom Room A")
    String location
) {

    public AssignRecruitingInterviewCommand toCommand(Long applicationId, Long assignedByMemberId) {
        return AssignRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .interviewerMemberId(interviewerMemberId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .location(location)
            .assignedByMemberId(assignedByMemberId)
            .build();
    }
}
