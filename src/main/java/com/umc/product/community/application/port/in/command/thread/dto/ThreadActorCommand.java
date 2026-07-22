package com.umc.product.community.application.port.in.command.thread.dto;

public record ThreadActorCommand(
    Long threadId,
    Long actorMemberId
) {

    public ThreadActorCommand {
        threadId = CommunityThreadCommandValidation.positiveId(threadId);
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
    }
}
