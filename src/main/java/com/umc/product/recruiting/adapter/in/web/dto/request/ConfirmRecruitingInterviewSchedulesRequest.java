package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 일정 일괄 확정 요청")
public record ConfirmRecruitingInterviewSchedulesRequest(
    @Schema(description = "확정할 면접 배정 목록", maxLength = 100)
    @NotEmpty @Size(max = ConfirmRecruitingInterviewSchedulesCommand.MAX_ASSIGNMENT_COUNT) List<@NotNull @Valid AssignmentRequest> assignments
) {

    public ConfirmRecruitingInterviewSchedulesCommand toCommand(Long roundId, Long requesterMemberId) {
        return ConfirmRecruitingInterviewSchedulesCommand.of(
            roundId,
            requesterMemberId,
            assignments.stream().map(AssignmentRequest::toCommand).toList()
        );
    }

    @Schema(description = "면접 일정 일괄 확정 항목")
    public record AssignmentRequest(
        @Schema(description = "지원서 ID", example = "40") @NotNull @Positive Long applicationId,
        @Schema(description = "면접 세션 ID", example = "10") @NotNull @Positive Long sessionId,
        @Schema(description = "면접 시작 시각") @NotNull Instant startsAt,
        @Schema(description = "확정 시점 연락처 스냅샷", maxLength = 2000)
        @NotBlank @Size(max = 2000) String contactSnapshot
    ) {
        private ConfirmRecruitingInterviewSchedulesCommand.Assignment toCommand() {
            return ConfirmRecruitingInterviewSchedulesCommand.Assignment.of(
                applicationId, sessionId, startsAt, contactSnapshot
            );
        }
    }
}
