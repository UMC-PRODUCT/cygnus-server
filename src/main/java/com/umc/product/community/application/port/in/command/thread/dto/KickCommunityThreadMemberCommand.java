package com.umc.product.community.application.port.in.command.thread.dto;

public record KickCommunityThreadMemberCommand(
    Long threadId,
    Long actorMemberId,
    Long memberId
) {

    public KickCommunityThreadMemberCommand {
        threadId = CommunityThreadCommandValidation.positiveId(threadId);
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
        memberId = CommunityThreadCommandValidation.positiveId(memberId);
        if (actorMemberId.equals(memberId)) {
            throw CommunityThreadCommandValidation.invalidCommand();
        }
    }
}
