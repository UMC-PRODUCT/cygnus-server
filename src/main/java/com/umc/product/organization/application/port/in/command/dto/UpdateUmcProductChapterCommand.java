package com.umc.product.organization.application.port.in.command.dto;

public record UpdateUmcProductChapterCommand(
    Long chapterId,
    Long requesterMemberId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean active
) {
    public static UpdateUmcProductChapterCommand of(
        Long chapterId,
        Long requesterMemberId,
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean active
    ) {
        return new UpdateUmcProductChapterCommand(
            chapterId, requesterMemberId, code, name, description, sortOrder, active
        );
    }
}
