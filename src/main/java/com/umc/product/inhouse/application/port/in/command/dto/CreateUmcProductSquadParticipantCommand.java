package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;

public record CreateUmcProductSquadParticipantCommand(
    Long squadId,
    Long requesterMemberId,
    Long umcProductMemberId,
    UmcProductSquadRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductSquadParticipantCommand of(
        Long squadId,
        Long requesterMemberId,
        Long umcProductMemberId,
        UmcProductSquadRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductSquadParticipantCommand(
            squadId, requesterMemberId, umcProductMemberId, role, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
