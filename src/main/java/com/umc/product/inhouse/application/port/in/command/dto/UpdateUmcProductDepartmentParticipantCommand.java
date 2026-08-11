package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UpdateUmcProductDepartmentParticipantCommand(
    Long departmentId,
    Long participantId,
    Long requesterMemberId,
    UmcProductDepartmentRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductDepartmentParticipantCommand of(
        Long departmentId,
        Long participantId,
        Long requesterMemberId,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductDepartmentParticipantCommand(
            departmentId, participantId, requesterMemberId, role, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
