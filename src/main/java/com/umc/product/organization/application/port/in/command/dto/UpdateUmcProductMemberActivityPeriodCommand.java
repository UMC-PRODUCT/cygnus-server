package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

public record UpdateUmcProductMemberActivityPeriodCommand(
    Long umcProductMemberId,
    Long activityPeriodId,
    Long requesterMemberId,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductMemberActivityPeriodCommand of(
        Long umcProductMemberId,
        Long activityPeriodId,
        Long requesterMemberId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductMemberActivityPeriodCommand(
            umcProductMemberId, activityPeriodId, requesterMemberId, startDate, endDate
        );
    }
}
