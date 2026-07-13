package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

public record UmcProductActivityPeriodCommand(
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductActivityPeriodCommand of(LocalDate startDate, LocalDate endDate) {
        return new UmcProductActivityPeriodCommand(startDate, endDate);
    }
}
