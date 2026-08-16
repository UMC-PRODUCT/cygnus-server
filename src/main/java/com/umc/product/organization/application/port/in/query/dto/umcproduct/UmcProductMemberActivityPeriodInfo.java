package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;

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
