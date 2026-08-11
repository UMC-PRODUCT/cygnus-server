package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentParticipationInfo;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductDepartmentParticipationResponse(
    Long departmentParticipantId,
    Long activityPeriodId,
    Long departmentId,
    UmcProductDepartmentResponse department,
    UmcProductDepartmentRole role,
    String roleName,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductDepartmentParticipationResponse from(UmcProductDepartmentParticipationInfo info) {
        return new UmcProductDepartmentParticipationResponse(
            info.departmentParticipantId(),
            info.activityPeriodId(),
            info.departmentId(),
            info.department() == null ? null : UmcProductDepartmentResponse.from(info.department()),
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
