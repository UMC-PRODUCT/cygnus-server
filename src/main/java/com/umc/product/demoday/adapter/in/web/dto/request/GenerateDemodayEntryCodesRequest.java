package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodeCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateDemodayEntryCodesRequest(
    @NotNull @Min(1) @Max(100) Integer count
) {
    public CreateDemodayEntryCodeCommand toCommand(Long pollId) {
        return new CreateDemodayEntryCodeCommand(pollId, count);
    }
}
