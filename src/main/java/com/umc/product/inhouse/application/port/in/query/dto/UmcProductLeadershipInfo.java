package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

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
