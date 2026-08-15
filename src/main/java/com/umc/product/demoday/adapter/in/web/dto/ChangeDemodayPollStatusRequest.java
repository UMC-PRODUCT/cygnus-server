package com.umc.product.demoday.adapter.in.web.dto;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;

public record ChangeDemodayPollStatusRequest(
    DemodayPollStatus status
) {
    public ChangeDemodayPollStatusCommand toCommand(
        Long pollId,
        Long memberId
    ) {
        return new ChangeDemodayPollStatusCommand(memberId, pollId, status);
    }
}
