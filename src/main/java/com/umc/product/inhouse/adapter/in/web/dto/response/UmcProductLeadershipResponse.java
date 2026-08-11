package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductLeadershipInfo;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductLeadershipResponse(
    Long leadershipId,
    Long activityPeriodId,
    UmcProductLeadershipRole role,
    String roleName,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductLeadershipResponse from(UmcProductLeadershipInfo info) {
        return new UmcProductLeadershipResponse(
            info.leadershipId(),
            info.activityPeriodId(),
            info.role(),
            info.roleName(),
            info.startDate(),
            info.endDate()
        );
    }
}
