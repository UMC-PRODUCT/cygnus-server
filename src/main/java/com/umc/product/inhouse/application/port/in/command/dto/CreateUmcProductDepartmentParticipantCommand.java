package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record CreateUmcProductDepartmentParticipantCommand(
    Long departmentId,
    Long requesterMemberId,
    Long umcProductMemberId,
    UmcProductDepartmentRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductDepartmentParticipantCommand of(
        Long departmentId,
        Long requesterMemberId,
        Long umcProductMemberId,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductDepartmentParticipantCommand(
            departmentId, requesterMemberId, umcProductMemberId, role, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
