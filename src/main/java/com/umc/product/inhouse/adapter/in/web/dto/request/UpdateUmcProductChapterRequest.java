package com.umc.product.inhouse.adapter.in.web.dto.request;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductChapterCommand;

import jakarta.validation.constraints.Size;

public record UpdateUmcProductChapterRequest(
    @Size(min = 1, max = 64) String code,
    @Size(min = 1, max = 100) String name,
    @Size(max = 1000) String description,
    Integer sortOrder,
    Boolean active
) {
    public UpdateUmcProductChapterCommand toCommand(Long chapterId, Long requesterMemberId) {
        return UpdateUmcProductChapterCommand.of(
            chapterId, requesterMemberId, code, name, description, sortOrder, active
        );
    }
}
