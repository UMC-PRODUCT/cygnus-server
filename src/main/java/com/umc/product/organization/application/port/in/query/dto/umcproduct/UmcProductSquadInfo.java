package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.UmcProductSquad;

public record UmcProductSquadInfo(
    Long squadId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static UmcProductSquadInfo from(UmcProductSquad squad) {
        return new UmcProductSquadInfo(
            squad.getId(),
            squad.getCode(),
            squad.getName(),
            squad.getDescription(),
            squad.getStartDate(),
            squad.getEndDate(),
            squad.getSortOrder(),
            squad.isActive()
        );
    }
}
