package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadMessageHistoryQuery(
    Long threadId,
    Long requesterMemberId,
    Long beforeMessageId,
    int limit
) {

    public CommunityThreadMessageHistoryQuery {
        threadId = requirePositive(threadId, "threadId");
        requesterMemberId = requirePositive(requesterMemberId, "requesterMemberId");
        beforeMessageId = requireOptionalPositive(beforeMessageId, "beforeMessageId");
        requireLimit(limit);
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Long requireOptionalPositive(Long value, String name) {
        return value == null ? null : requirePositive(value, name);
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
}
