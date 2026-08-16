package com.umc.product.organization.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateUmcProductLeadershipRequest(
    @NotNull UmcProductLeadershipRole role,
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate
) {
    public UpdateUmcProductLeadershipCommand toCommand(
        Long umcProductMemberId,
        Long leadershipId,
        Long requesterMemberId
    ) {
        return UpdateUmcProductLeadershipCommand.of(
            umcProductMemberId, leadershipId, requesterMemberId, role, startDate, endDate
        );
    }
}
