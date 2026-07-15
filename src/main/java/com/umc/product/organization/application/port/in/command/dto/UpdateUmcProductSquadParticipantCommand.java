package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;

public record UpdateUmcProductSquadParticipantCommand(
    Long squadId,
    Long participantId,
    Long requesterMemberId,
    UmcProductSquadRole role,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductSquadParticipantCommand of(
        Long squadId,
        Long participantId,
        Long requesterMemberId,
        UmcProductSquadRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductSquadParticipantCommand(
            squadId, participantId, requesterMemberId, role, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
