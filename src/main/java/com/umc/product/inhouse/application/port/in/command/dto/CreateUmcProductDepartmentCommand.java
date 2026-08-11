package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

public record CreateUmcProductDepartmentCommand(
    Long requesterMemberId,
    String code,
    String name,
    String description,
    Long parentDepartmentId,
    LocalDate startDate,
    LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static CreateUmcProductDepartmentCommand of(
        Long requesterMemberId,
        String code,
        String name,
        String description,
        Long parentDepartmentId,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean active
    ) {
        return new CreateUmcProductDepartmentCommand(
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
