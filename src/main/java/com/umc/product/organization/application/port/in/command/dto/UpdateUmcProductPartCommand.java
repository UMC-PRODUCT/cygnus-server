package com.umc.product.organization.application.port.in.command.dto;

public record UpdateUmcProductPartCommand(
    Long partId,
    Long requesterMemberId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean active
) {
    public static UpdateUmcProductPartCommand of(
        Long partId,
        Long requesterMemberId,
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean active
    ) {
        return new UpdateUmcProductPartCommand(partId, requesterMemberId, code, name, description, sortOrder, active);
    }
}
