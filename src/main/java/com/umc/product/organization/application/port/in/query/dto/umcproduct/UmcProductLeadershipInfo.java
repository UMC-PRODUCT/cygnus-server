package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

public record UmcProductLeadershipInfo(
    Long leadershipId,
    Long activityPeriodId,
    UmcProductLeadershipRole role,
    String roleName,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductLeadershipInfo from(UmcProductLeadership leadership) {
        return new UmcProductLeadershipInfo(
            leadership.getId(),
            leadership.getMemberActivityPeriod().getId(),
            leadership.getRole(),
            leadership.getRole().getDisplayName(),
            leadership.getStartDate(),
            leadership.getEndDate()
        );
    }
}
