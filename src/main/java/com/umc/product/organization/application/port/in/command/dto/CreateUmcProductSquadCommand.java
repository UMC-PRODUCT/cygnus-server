package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

public record CreateUmcProductSquadCommand(
    Long requesterMemberId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    int sortOrder,
    boolean active
) {
    public static CreateUmcProductSquadCommand of(
        Long requesterMemberId,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean active
    ) {
        return new CreateUmcProductSquadCommand(
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
