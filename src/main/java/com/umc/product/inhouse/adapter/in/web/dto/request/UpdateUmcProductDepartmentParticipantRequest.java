package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentParticipantCommand;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUmcProductDepartmentParticipantRequest(
    @NotNull UmcProductDepartmentRole role,
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
    public UpdateUmcProductDepartmentParticipantCommand toCommand(
        Long departmentId,
        Long participantId,
        Long requesterMemberId
    ) {
        return UpdateUmcProductDepartmentParticipantCommand.of(
            departmentId,
            participantId,
            requesterMemberId,
            role,
            position,
            responsibilityTitle,
            responsibilityDescription,
            startDate,
            endDate
        );
    }
}
