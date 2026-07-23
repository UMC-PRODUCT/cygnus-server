package com.umc.product.community.application.port.in.command.thread.message.dto;

public record UpdateCommunityThreadReadCommand(
    Long threadId,
    Long memberId,
    Long lastReadMessageId
) {

    public UpdateCommunityThreadReadCommand {
        threadId = positiveId(threadId, "threadId");
        memberId = positiveId(memberId, "memberId");
        lastReadMessageId = positiveId(lastReadMessageId, "lastReadMessageId");
    }

    private static Long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
