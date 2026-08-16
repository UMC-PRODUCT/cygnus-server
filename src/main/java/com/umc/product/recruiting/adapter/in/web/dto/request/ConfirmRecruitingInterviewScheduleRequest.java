package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 일정 확정 요청")
public record ConfirmRecruitingInterviewScheduleRequest(
    @Schema(description = "면접 세션 ID", example = "10") @NotNull @Positive Long sessionId,
    @Schema(description = "면접 시작 시각") @NotNull Instant startsAt,
    @Schema(description = "면접 종료 시각") @NotNull Instant endsAt,
    @Schema(description = "면접 장소 또는 접속 링크", example = "온라인 회의실 A")
    @NotBlank @Size(max = 255) String location,
    @Schema(description = "확정 시점 연락처 스냅샷", example = "문의: recruit@example.org", maxLength = 2000)
    @NotBlank @Size(max = 2000) String contactSnapshot
) {

    @AssertTrue(message = "면접 종료 시각은 시작 시각보다 늦어야 합니다.") public boolean isPeriodValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }

    public ConfirmRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
        return ConfirmRecruitingInterviewScheduleCommand.of(
            applicationId,
            requesterMemberId,
            sessionId,
            startsAt,
            endsAt,
            location,
            contactSnapshot
        );
    }
}
