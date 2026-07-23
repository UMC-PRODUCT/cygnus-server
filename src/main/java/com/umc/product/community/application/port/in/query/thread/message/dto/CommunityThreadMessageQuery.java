package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadMessageQuery(
    Long threadId,
    Long requesterMemberId,
    Long messageId
) {

    public CommunityThreadMessageQuery {
        threadId = requirePositive(threadId, "threadId");
        requesterMemberId = requirePositive(requesterMemberId, "requesterMemberId");
        messageId = requirePositive(messageId, "messageId");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
