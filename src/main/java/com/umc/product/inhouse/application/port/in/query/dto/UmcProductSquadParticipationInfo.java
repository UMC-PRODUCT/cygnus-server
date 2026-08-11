package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductSquadParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;

public record UmcProductSquadParticipationInfo(
    Long squadParticipantId,
    Long activityPeriodId,
    Long squadId,
    UmcProductSquadInfo squad,
    UmcProductSquadRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductSquadParticipationInfo from(
        UmcProductSquadParticipant participant,
        UmcProductSquadInfo squad
    ) {
        return new UmcProductSquadParticipationInfo(
            participant.getId(),
            participant.getMemberActivityPeriod().getId(),
            participant.getSquad().getId(),
            squad,
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
