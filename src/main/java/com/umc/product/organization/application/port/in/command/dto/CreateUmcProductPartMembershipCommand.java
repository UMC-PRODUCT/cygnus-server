package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record CreateUmcProductPartMembershipCommand(
    Long umcProductMemberId,
    Long requesterMemberId,
    Long partId,
    UmcProductPartRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductPartMembershipCommand of(
        Long umcProductMemberId,
        Long requesterMemberId,
        Long partId,
        UmcProductPartRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductPartMembershipCommand(
            umcProductMemberId, requesterMemberId, partId, role, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
