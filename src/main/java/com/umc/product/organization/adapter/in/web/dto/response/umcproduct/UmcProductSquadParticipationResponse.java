package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadParticipationInfo;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductSquadParticipationResponse(
    Long squadParticipantId,
    Long activityPeriodId,
    Long squadId,
    UmcProductSquadResponse squad,
    UmcProductSquadRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductSquadParticipationResponse from(UmcProductSquadParticipationInfo info) {
        return new UmcProductSquadParticipationResponse(
            info.squadParticipantId(),
            info.activityPeriodId(),
            info.squadId(),
            info.squad() == null ? null : UmcProductSquadResponse.from(info.squad()),
            info.role(),
            info.roleName(),
            info.position(),
            info.positionName(),
            info.responsibilityTitle(),
            info.responsibilityDescription(),
            info.startDate(),
            info.endDate()
        );
    }
}
