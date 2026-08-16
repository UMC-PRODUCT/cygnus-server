package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadReadMutationInfo(
    Long threadId,
    Long memberId,
    Long lastReadMessageId,
    boolean deduplicated
) {

    public CommunityThreadReadMutationInfo {
        threadId = requirePositive(threadId, "threadId");
        memberId = requirePositive(memberId, "memberId");
        lastReadMessageId = requirePositive(lastReadMessageId, "lastReadMessageId");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
