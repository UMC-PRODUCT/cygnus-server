package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.UmcProductActivityPeriodCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UmcProductActivityPeriodRequest(
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate
) {
    public UmcProductActivityPeriodCommand toCommand() {
        return UmcProductActivityPeriodCommand.of(startDate, endDate);
    }
}
