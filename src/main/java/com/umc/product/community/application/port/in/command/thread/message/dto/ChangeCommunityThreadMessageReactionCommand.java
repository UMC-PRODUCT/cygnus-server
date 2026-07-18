package com.umc.product.community.application.port.in.command.thread.message.dto;

public record ChangeCommunityThreadMessageReactionCommand(
    Long threadId,
    Long messageId,
    Long memberId,
    String emoji
) {

    public ChangeCommunityThreadMessageReactionCommand {
        threadId = positiveId(threadId, "threadId");
        messageId = positiveId(messageId, "messageId");
        memberId = positiveId(memberId, "memberId");
    }

    private static Long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
