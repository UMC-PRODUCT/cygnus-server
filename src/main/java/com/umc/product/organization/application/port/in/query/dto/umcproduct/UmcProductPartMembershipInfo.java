package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record UmcProductPartMembershipInfo(
    Long partMembershipId,
    Long activityPeriodId,
    Long partId,
    UmcProductPartInfo part,
    UmcProductPartRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductPartMembershipInfo from(
        UmcProductPartMembership membership,
        UmcProductPartInfo part
    ) {
        return new UmcProductPartMembershipInfo(
            membership.getId(),
            membership.getMemberActivityPeriod().getId(),
            membership.getPart().getId(),
            part,
            membership.getRole(),
            membership.getRole().getDisplayName(),
            membership.getPosition(),
            membership.getPosition().getDisplayName(),
            membership.getResponsibilityTitle(),
            membership.getResponsibilityDescription(),
            membership.getStartDate(),
            membership.getEndDate()
        );
    }
}
