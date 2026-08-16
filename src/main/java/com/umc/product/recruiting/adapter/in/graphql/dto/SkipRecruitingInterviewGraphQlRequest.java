package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

public record SkipRecruitingInterviewGraphQlRequest(
    String reason
) {

    public SkipRecruitingInterviewCommand toCommand(Long applicationId, Long skippedByMemberId) {
        return SkipRecruitingInterviewCommand.builder()
            .applicationId(applicationId)
            .skippedByMemberId(skippedByMemberId)
            .reason(reason)
            .build();
    }
}
