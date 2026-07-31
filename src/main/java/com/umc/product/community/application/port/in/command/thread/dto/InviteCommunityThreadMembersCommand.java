package com.umc.product.community.application.port.in.command.thread.dto;

import java.util.List;

public record InviteCommunityThreadMembersCommand(
    Long threadId,
    Long actorMemberId,
    List<Long> memberIds
) {

    public InviteCommunityThreadMembersCommand {
        threadId = CommunityThreadCommandValidation.positiveId(threadId);
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
        memberIds = CommunityThreadCommandValidation.uniqueIds(memberIds, false);
    }
}
