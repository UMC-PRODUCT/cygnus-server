package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "지원자 면접 가능 일정 응답 제출 요청")
public record SubmitRecruitingInterviewAvailabilityRequest(
    @Schema(description = "Form 가능 일정 FormResponse ID", example = "700")
    @NotNull @Positive Long availabilityFormResponseId
) {

    public SubmitRecruitingInterviewAvailabilityCommand toCommand(Long applicationId, Long requesterMemberId) {
        return SubmitRecruitingInterviewAvailabilityCommand.of(
            applicationId,
            requesterMemberId,
            availabilityFormResponseId
        );
    }
}
