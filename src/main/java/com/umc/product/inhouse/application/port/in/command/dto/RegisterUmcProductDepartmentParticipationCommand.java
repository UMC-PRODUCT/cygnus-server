package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record RegisterUmcProductDepartmentParticipationCommand(
    Long departmentId,
    UmcProductDepartmentRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public CreateUmcProductDepartmentParticipantCommand toCreateCommand(
        Long requesterMemberId,
        Long umcProductMemberId
    ) {
        return CreateUmcProductDepartmentParticipantCommand.of(
            departmentId,
            requesterMemberId,
            umcProductMemberId,
            role,
            position,
            responsibilityTitle,
            responsibilityDescription,
            startDate,
            endDate
        );
    }
}
