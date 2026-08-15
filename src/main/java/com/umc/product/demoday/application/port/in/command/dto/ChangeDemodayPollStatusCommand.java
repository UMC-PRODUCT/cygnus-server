package com.umc.product.demoday.application.port.in.command.dto;

import com.umc.product.demoday.domain.enums.DemodayPollStatus;

public record ChangeDemodayPollStatusCommand(
    Long memberId,
    Long pollId,
    DemodayPollStatus status
) {
}
