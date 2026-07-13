package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;

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
