package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;

public record LinkRecruitingApplicationFormGraphQlRequest(
    Long formId
) {

    public LinkRecruitingApplicationFormCommand toCommand(Long seasonId, Long roundId) {
        return LinkRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .formId(formId)
            .build();
    }
}
