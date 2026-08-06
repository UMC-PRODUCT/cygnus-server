package com.umc.product.community.application.port.in.command.thread.dto;

import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

public record ChangeCommunityThreadMemberRoleCommand(
    Long threadId,
    Long actorMemberId,
    Long memberId,
    CommunityThreadMemberRole role
) {

    public ChangeCommunityThreadMemberRoleCommand {
        threadId = CommunityThreadCommandValidation.positiveId(threadId);
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
        memberId = CommunityThreadCommandValidation.positiveId(memberId);
        role = CommunityThreadCommandValidation.required(role);
    }
}
