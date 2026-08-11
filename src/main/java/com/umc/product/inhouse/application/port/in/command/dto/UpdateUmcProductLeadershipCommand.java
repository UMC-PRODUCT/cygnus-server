package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

public record UpdateUmcProductLeadershipCommand(
    Long umcProductMemberId,
    Long leadershipId,
    Long requesterMemberId,
    UmcProductLeadershipRole role,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductLeadershipCommand of(
        Long umcProductMemberId,
        Long leadershipId,
        Long requesterMemberId,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductLeadershipCommand(
            umcProductMemberId, leadershipId, requesterMemberId, role, startDate, endDate
        );
    }
}
