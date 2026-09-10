package com.umc.product.organization.application.port.in.command.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record CreateGisuCommand(
    Long generation,
    Instant startAt,
    Instant endAt
) {
}
