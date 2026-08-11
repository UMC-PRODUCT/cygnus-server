package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

public record CreateUmcProductMemberActivityPeriodCommand(
    Long umcProductMemberId,
    Long requesterMemberId,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductMemberActivityPeriodCommand of(
        Long umcProductMemberId,
        Long requesterMemberId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductMemberActivityPeriodCommand(
            umcProductMemberId, requesterMemberId, startDate, endDate
        );
    }
}
