package com.umc.product.member.application.port.in.command.dto;

import com.umc.product.common.domain.enums.MemberStatus;

public record UpdateMemberCommand(
        Long memberId,
        String newNickname,
        String newProfileImageId,
        MemberStatus newStatus,
        Long requesterMemberId

) {
    public UpdateMemberCommand(
            Long memberId,
            String newNickname,
            String newProfileImageId,
            MemberStatus newStatus
    ) {
        this(memberId, newNickname, newProfileImageId, newStatus, memberId);
    }

    public static UpdateMemberCommand forProfileUpdate(
            Long memberId,
            String newProfileImageId
    ) {
        return new UpdateMemberCommand(
                memberId,
                null,
                newProfileImageId,
                null,
                memberId
        );
    }

    public static UpdateMemberCommand forProfileUpdate(
            Long memberId,
            Long requesterMemberId,
            String newProfileImageId
    ) {
        return new UpdateMemberCommand(
                memberId,
                null,
                newProfileImageId,
                null,
                requesterMemberId
        );
    }
}
