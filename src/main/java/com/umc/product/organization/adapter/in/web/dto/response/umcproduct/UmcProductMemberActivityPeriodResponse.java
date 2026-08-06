package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberActivityPeriodInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductMemberActivityPeriodResponse(
    Long activityPeriodId,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductMemberActivityPeriodResponse from(UmcProductMemberActivityPeriodInfo info) {
        return new UmcProductMemberActivityPeriodResponse(
            info.activityPeriodId(), info.startDate(), info.endDate()
        );
    }
}
