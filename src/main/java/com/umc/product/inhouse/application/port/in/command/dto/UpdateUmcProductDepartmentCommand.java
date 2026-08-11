package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

public record UpdateUmcProductDepartmentCommand(
    Long departmentId,
    Long requesterMemberId,
    String code,
    String name,
    String description,
    Long parentDepartmentId,
    LocalDate startDate,
    LocalDate endDate,
    Integer sortOrder,
    Boolean active
) {
    public static UpdateUmcProductDepartmentCommand of(
        Long departmentId,
        Long requesterMemberId,
        String code,
        String name,
        String description,
        Long parentDepartmentId,
        LocalDate startDate,
        LocalDate endDate,
        Integer sortOrder,
        Boolean active
    ) {
        return new UpdateUmcProductDepartmentCommand(
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
