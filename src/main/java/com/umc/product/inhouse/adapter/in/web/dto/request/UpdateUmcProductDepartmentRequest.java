package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUmcProductDepartmentRequest(
    @Size(max = 64) String code,
    @Size(max = 100) String name,
    @Size(max = 1000) String description,
    Long parentDepartmentId,
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate,
    Integer sortOrder,
    Boolean active
) {
    public UpdateUmcProductDepartmentCommand toCommand(Long departmentId, Long requesterMemberId) {
        return UpdateUmcProductDepartmentCommand.of(
            departmentId,
            requesterMemberId,
            code,
            name,
            description,
            parentDepartmentId,
            startDate,
            endDate,
            sortOrder,
            active
        );
    }
}
