package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateUmcProductMemberActivityPeriodRequest(
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate
) {
    public UpdateUmcProductMemberActivityPeriodCommand toCommand(
        Long umcProductMemberId,
        Long activityPeriodId,
        Long requesterMemberId
    ) {
        return UpdateUmcProductMemberActivityPeriodCommand.of(
            umcProductMemberId, activityPeriodId, requesterMemberId, startDate, endDate
        );
    }
}
