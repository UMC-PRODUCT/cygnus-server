package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadParticipantCommand;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductSquadParticipantRequest(
    @NotNull Long umcProductMemberId,
    @NotNull UmcProductSquadRole role,
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
    public CreateUmcProductSquadParticipantCommand toCommand(Long squadId, Long requesterMemberId) {
        return CreateUmcProductSquadParticipantCommand.of(
            squadId,
            requesterMemberId,
            umcProductMemberId,
            role,
            position,
            responsibilityTitle,
            responsibilityDescription,
            startDate,
            endDate
        );
    }
}
