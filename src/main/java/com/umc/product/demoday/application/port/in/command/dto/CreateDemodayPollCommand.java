package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

public record CreateDemodayPollCommand(
    Long memberId,
    Long gisuId,
    String name,
    Instant opensAt,
    Instant closesAt
) {
}
