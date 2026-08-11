package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UmcProductDepartmentParticipationInfo(
    Long departmentParticipantId,
    Long activityPeriodId,
    Long departmentId,
    UmcProductDepartmentInfo department,
    UmcProductDepartmentRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductDepartmentParticipationInfo from(
        UmcProductDepartmentParticipant participant,
        UmcProductDepartmentInfo department
    ) {
        return new UmcProductDepartmentParticipationInfo(
            participant.getId(),
            participant.getMemberActivityPeriod().getId(),
            participant.getDepartment().getId(),
            department,
            participant.getRole(),
            participant.getRole().getDisplayName(),
            participant.getPosition(),
            participant.getPosition().getDisplayName(),
            participant.getResponsibilityTitle(),
            participant.getResponsibilityDescription(),
            participant.getStartDate(),
            participant.getEndDate()
        );
    }
}
