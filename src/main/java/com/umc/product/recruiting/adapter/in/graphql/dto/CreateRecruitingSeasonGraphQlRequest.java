package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

public record CreateRecruitingSeasonGraphQlRequest(
    Long gisuId,
    Long schoolId
) {

    public CreateRecruitingSeasonCommand toCommand() {
        return CreateRecruitingSeasonCommand.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .build();
    }
}
