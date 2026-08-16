package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.demoday.domain.enums.DemodayPollStatus;

public record DemodayPollInfo(
        Long pollId,
        String name,
        Instant opensAt,
        Instant closesAt,
        DemodayPollStatus status
) {
}
