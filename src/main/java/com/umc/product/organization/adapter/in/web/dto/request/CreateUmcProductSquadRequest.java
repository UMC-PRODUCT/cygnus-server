package com.umc.product.organization.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductSquadCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductSquadRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate,
    Integer sortOrder,
    Boolean active
) {
    public CreateUmcProductSquadCommand toCommand(Long requesterMemberId) {
        return CreateUmcProductSquadCommand.of(
            requesterMemberId,
            code,
            name,
            description,
            startDate,
            endDate,
            sortOrder == null ? 0 : sortOrder,
            !Boolean.FALSE.equals(active)
        );
    }
}
