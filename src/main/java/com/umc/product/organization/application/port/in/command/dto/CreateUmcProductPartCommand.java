package com.umc.product.organization.application.port.in.command.dto;

public record CreateUmcProductPartCommand(
    Long requesterMemberId,
    Long chapterId,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static CreateUmcProductPartCommand of(
        Long requesterMemberId,
        Long chapterId,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean active
    ) {
        return new CreateUmcProductPartCommand(
            requesterMemberId, chapterId, code, name, description, sortOrder, active
        );
    }
}
