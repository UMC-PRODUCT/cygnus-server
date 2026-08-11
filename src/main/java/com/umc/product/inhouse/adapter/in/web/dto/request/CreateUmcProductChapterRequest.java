package com.umc.product.inhouse.adapter.in.web.dto.request;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductChapterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUmcProductChapterRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    Integer sortOrder,
    Boolean active
) {
    public CreateUmcProductChapterCommand toCommand(Long requesterMemberId) {
        return CreateUmcProductChapterCommand.of(
            requesterMemberId,
            code,
            name,
            description,
            sortOrder == null ? 0 : sortOrder,
            !Boolean.FALSE.equals(active)
        );
    }
}
