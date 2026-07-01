package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

public record SkipRecruitingInterviewRequest(
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
