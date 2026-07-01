package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateRecruitingRoundStatusRequest(
    @NotNull RecruitingRoundStatus status
) {

    public UpdateRecruitingRoundStatusCommand toCommand(Long roundId) {
        return UpdateRecruitingRoundStatusCommand.builder()
            .roundId(roundId)
            .status(status)
            .build();
    }
}
