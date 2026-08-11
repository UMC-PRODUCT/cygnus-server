package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberActivityPeriodInfo;

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
