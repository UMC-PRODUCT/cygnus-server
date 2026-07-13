package com.umc.product.organization.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartMembershipCommand;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductPartMembershipRequest(
    @NotNull Long partId,
    @NotNull UmcProductPartRole role,
    @NotNull UmcProductPosition position,
    @Size(max = 200) String responsibilityTitle,
    @Size(max = 1000) String responsibilityDescription,
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate
) {
    public CreateUmcProductPartMembershipCommand toCommand(
        Long umcProductMemberId,
        Long requesterMemberId
    ) {
        return CreateUmcProductPartMembershipCommand.of(
            umcProductMemberId,
            requesterMemberId,
            partId,
            role,
            position,
            responsibilityTitle,
            responsibilityDescription,
            startDate,
            endDate
        );
    }
}
