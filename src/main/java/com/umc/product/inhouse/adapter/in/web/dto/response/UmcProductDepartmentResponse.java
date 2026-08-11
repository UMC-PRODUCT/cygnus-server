package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductDepartmentResponse(
    Long departmentId,
    String code,
    String name,
    String description,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static UmcProductDepartmentResponse from(UmcProductDepartmentInfo info) {
        return new UmcProductDepartmentResponse(
            info.departmentId(),
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
