package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUmcProductSquadRequest(
    @Size(max = 64) String code,
    @Size(max = 100) String name,
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
    public UpdateUmcProductSquadCommand toCommand(Long squadId, Long requesterMemberId) {
        return UpdateUmcProductSquadCommand.of(
            squadId,
            requesterMemberId,
            code,
            name,
            description,
            startDate,
            endDate,
            sortOrder,
            active
        );
    }
}
