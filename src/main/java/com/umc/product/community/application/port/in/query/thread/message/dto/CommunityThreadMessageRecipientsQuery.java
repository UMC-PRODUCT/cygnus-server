package com.umc.product.community.application.port.in.query.thread.message.dto;

import java.util.List;
import java.util.Objects;

public record CommunityThreadMessageRecipientsQuery(
    Long threadId,
    Long messageId,
    List<Long> recipientMemberIds
) {

    public CommunityThreadMessageRecipientsQuery {
        threadId = requirePositive(threadId, "threadId");
        messageId = requirePositive(messageId, "messageId");
        recipientMemberIds = Objects.requireNonNull(
            recipientMemberIds,
            "recipientMemberIds must not be null"
        ).stream()
            .map(memberId -> requirePositive(memberId, "recipientMemberId"))
            .distinct()
            .toList();
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
