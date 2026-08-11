package com.umc.product.inhouse.adapter.in.web.dto.request;

import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;

import jakarta.validation.constraints.Size;

public record UpdateUmcProductMemberProfileRequest(
    @Size(max = 2000) String introduction,
    String profileImageId
) {
    public UpdateUmcProductMemberProfileCommand toCommand(Long umcProductMemberId, Long requesterMemberId) {
        return UpdateUmcProductMemberProfileCommand.of(
            umcProductMemberId,
            requesterMemberId,
            introduction,
            profileImageId
        );
    }
}
