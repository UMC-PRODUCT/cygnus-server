package com.umc.product.organization.adapter.in.web.dto.request;

import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductPartCommand;

import jakarta.validation.constraints.Size;

public record UpdateUmcProductPartRequest(
    @Size(min = 1, max = 64) String code,
    @Size(min = 1, max = 100) String name,
    @Size(max = 1000) String description,
    Integer sortOrder,
    Boolean active
) {
    public UpdateUmcProductPartCommand toCommand(Long partId, Long requesterMemberId) {
        return UpdateUmcProductPartCommand.of(
            partId, requesterMemberId, code, name, description, sortOrder, active
        );
    }
}
