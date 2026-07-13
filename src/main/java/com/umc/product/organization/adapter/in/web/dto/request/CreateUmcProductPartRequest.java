package com.umc.product.organization.adapter.in.web.dto.request;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductPartRequest(
    @NotNull Long chapterId,
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    Integer sortOrder,
    Boolean active
) {
    public CreateUmcProductPartCommand toCommand(Long requesterMemberId) {
        return CreateUmcProductPartCommand.of(
            requesterMemberId,
            chapterId,
            code,
            name,
            description,
            sortOrder == null ? 0 : sortOrder,
            !Boolean.FALSE.equals(active)
        );
    }
}
