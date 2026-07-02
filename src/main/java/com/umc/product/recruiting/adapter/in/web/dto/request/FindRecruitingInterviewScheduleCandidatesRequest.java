package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "면접 일정 후보 조회 요청")
public record FindRecruitingInterviewScheduleCandidatesRequest(
    @Schema(description = "가능 일정 응답을 포함한 form ID", example = "500")
    @NotNull Long formId,
    @Schema(description = "일정 조율 대상 form 응답 ID 목록", example = "[10001,10002,10003]")
    @NotEmpty List<Long> formResponseIds
) {

    public FindRecruitingInterviewScheduleCandidatesCommand toCommand() {
        return FindRecruitingInterviewScheduleCandidatesCommand.builder()
            .formId(formId)
            .formResponseIds(formResponseIds)
            .build();
    }
}
