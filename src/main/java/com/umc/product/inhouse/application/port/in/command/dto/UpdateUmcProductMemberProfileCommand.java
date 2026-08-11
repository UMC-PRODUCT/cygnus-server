package com.umc.product.inhouse.application.port.in.command.dto;

public record UpdateUmcProductMemberProfileCommand(
    Long umcProductMemberId,
    Long requesterMemberId,
    String introduction,
    String profileImageId
) {
    public static UpdateUmcProductMemberProfileCommand of(
        Long umcProductMemberId,
        Long requesterMemberId,
        String introduction,
        String profileImageId
    ) {
        return new UpdateUmcProductMemberProfileCommand(
            umcProductMemberId,
            requesterMemberId,
            introduction,
            profileImageId
        );
    }
}
