package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;

public record FindRecruitingInterviewScheduleCandidatesGraphQlRequest(
    Long formId,
    List<Long> formResponseIds
) {

    public FindRecruitingInterviewScheduleCandidatesCommand toCommand() {
        return FindRecruitingInterviewScheduleCandidatesCommand.builder()
            .formId(formId)
            .formResponseIds(formResponseIds == null ? List.of() : formResponseIds)
            .build();
    }
}
