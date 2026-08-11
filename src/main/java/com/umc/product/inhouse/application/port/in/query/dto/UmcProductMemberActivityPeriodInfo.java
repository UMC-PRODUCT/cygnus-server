package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;

public record UmcProductMemberActivityPeriodInfo(
    Long activityPeriodId,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductMemberActivityPeriodInfo from(UmcProductMemberActivityPeriod activityPeriod) {
        return new UmcProductMemberActivityPeriodInfo(
            activityPeriod.getId(), activityPeriod.getStartDate(), activityPeriod.getEndDate()
        );
    }
}
