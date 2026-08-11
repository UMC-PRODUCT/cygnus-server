package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductDepartment;

public record UmcProductDepartmentInfo(
    Long departmentId,
    String code,
    String name,
    String description,
    Long parentDepartmentId,
    LocalDate startDate,
    LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static UmcProductDepartmentInfo from(UmcProductDepartment department) {
        return new UmcProductDepartmentInfo(
            department.getId(),
            department.getCode(),
            department.getName(),
            department.getDescription(),
            department.getParent() == null ? null : department.getParent().getId(),
            department.getStartDate(),
            department.getEndDate(),
            department.getSortOrder(),
            department.isActive()
        );
    }
}
