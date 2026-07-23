package com.umc.product.community.application.port.in.command.thread.message.dto;

public record TombstoneCommunityThreadMessageCommand(
    Long threadId,
    Long messageId,
    Long requesterMemberId
) {

    public TombstoneCommunityThreadMessageCommand {
        threadId = positiveId(threadId, "threadId");
        messageId = positiveId(messageId, "messageId");
        requesterMemberId = positiveId(requesterMemberId, "requesterMemberId");
    }

    private static Long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
