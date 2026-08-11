package com.umc.product.inhouse.application.port.in.command.dto;

public record CreateUmcProductChapterCommand(
    Long requesterMemberId,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static CreateUmcProductChapterCommand of(
        Long requesterMemberId,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean active
    ) {
        return new CreateUmcProductChapterCommand(requesterMemberId, code, name, description, sortOrder, active);
    }
}
