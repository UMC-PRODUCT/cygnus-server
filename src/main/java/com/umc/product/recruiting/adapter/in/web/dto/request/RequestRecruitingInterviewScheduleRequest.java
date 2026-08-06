package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 가능 일정 제출 요청 생성")
public record RequestRecruitingInterviewScheduleRequest(
    @Schema(description = "지원자에게 표시할 연락처 스냅샷", example = "문의: recruit@example.org", maxLength = 2000)
    @NotBlank @Size(max = 2000) String contactSnapshot
) {

    public RequestRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
        return RequestRecruitingInterviewScheduleCommand.of(applicationId, requesterMemberId, contactSnapshot);
    }
}
