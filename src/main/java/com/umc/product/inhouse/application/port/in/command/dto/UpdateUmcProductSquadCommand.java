package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

public record UpdateUmcProductSquadCommand(
    Long squadId,
    Long requesterMemberId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    Integer sortOrder,
    Boolean active
) {
    public static UpdateUmcProductSquadCommand of(
        Long squadId,
        Long requesterMemberId,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Integer sortOrder,
        Boolean active
    ) {
        return new UpdateUmcProductSquadCommand(
            squadId,
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
