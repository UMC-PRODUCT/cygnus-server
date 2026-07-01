package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record FindRecruitingInterviewScheduleCandidatesRequest(
    @NotNull Long formId,
    @NotEmpty List<Long> formResponseIds
) {

    public FindRecruitingInterviewScheduleCandidatesCommand toCommand() {
        return FindRecruitingInterviewScheduleCandidatesCommand.builder()
            .formId(formId)
            .formResponseIds(formResponseIds)
            .build();
    }
}
