package com.umc.product.organization.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.organization.application.port.in.command.dto.CreateGisuCommand;

import jakarta.validation.constraints.NotNull;

public record CreateGisuRequest(
    @NotNull Long generation,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {
    public CreateGisuCommand toCommand() {
        return new CreateGisuCommand(generation, startAt, endAt);
    }
}
