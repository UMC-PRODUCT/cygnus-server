package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartMembershipInfo;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductPartMembershipResponse(
    Long partMembershipId,
    Long activityPeriodId,
    Long partId,
    UmcProductPartResponse part,
    UmcProductPartRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductPartMembershipResponse from(UmcProductPartMembershipInfo info) {
        return new UmcProductPartMembershipResponse(
            info.partMembershipId(),
            info.activityPeriodId(),
            info.partId(),
            info.part() == null ? null : UmcProductPartResponse.from(info.part()),
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
