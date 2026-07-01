package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

import jakarta.validation.constraints.NotNull;

public record CreateRecruitingSeasonRequest(
    @NotNull Long gisuId,
    @NotNull Long schoolId
) {

    public CreateRecruitingSeasonCommand toCommand() {
        return CreateRecruitingSeasonCommand.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .build();
    }
}
