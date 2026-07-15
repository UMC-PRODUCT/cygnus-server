package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

public record CreateUmcProductLeadershipCommand(
    Long umcProductMemberId,
    Long requesterMemberId,
    UmcProductLeadershipRole role,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductLeadershipCommand of(
        Long umcProductMemberId,
        Long requesterMemberId,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductLeadershipCommand(
            umcProductMemberId, requesterMemberId, role, startDate, endDate
        );
    }
}
