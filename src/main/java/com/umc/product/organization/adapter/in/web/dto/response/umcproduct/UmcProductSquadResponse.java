package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductSquadResponse(
    Long squadId,
    String code,
    String name,
    String description,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static UmcProductSquadResponse from(UmcProductSquadInfo info) {
        return new UmcProductSquadResponse(
            info.squadId(),
            info.code(),
            info.name(),
            info.description(),
            info.startDate(),
            info.endDate(),
            info.sortOrder(),
            info.active()
        );
    }
}
