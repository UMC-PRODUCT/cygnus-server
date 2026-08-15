package com.umc.product.demoday.adapter.in.web.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;

import jakarta.validation.constraints.NotBlank;

public record CreateDemodayPollRequest(
    @NotNull Long gisuId,
    @NotBlank String name,
    @NotNull Instant  opensAt,
    @NotNull Instant closesAt
) {
    public CreateDemodayPollCommand toCommand(Long memberId) {
        return new CreateDemodayPollCommand(memberId, gisuId, name, opensAt, closesAt);
    }
}
