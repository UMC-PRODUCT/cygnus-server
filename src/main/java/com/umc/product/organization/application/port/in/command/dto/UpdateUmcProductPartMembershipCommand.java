package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record UpdateUmcProductPartMembershipCommand(
    Long umcProductMemberId,
    Long partMembershipId,
    Long requesterMemberId,
    Long partId,
    UmcProductPartRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductPartMembershipCommand of(
        Long umcProductMemberId,
        Long partMembershipId,
        Long requesterMemberId,
        Long partId,
        UmcProductPartRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductPartMembershipCommand(
            umcProductMemberId, partMembershipId, requesterMemberId, partId, role, position,
            responsibilityTitle, responsibilityDescription, startDate, endDate
        );
    }
}
