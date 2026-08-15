package com.umc.product.demoday.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;

public record ChangeDemodayPollStatusRequest(
    @NotNull DemodayPollStatus status
) {
    public ChangeDemodayPollStatusCommand toCommand(
        Long pollId,
        Long memberId
    ) {
        return new ChangeDemodayPollStatusCommand(memberId, pollId, status);
    }
}
