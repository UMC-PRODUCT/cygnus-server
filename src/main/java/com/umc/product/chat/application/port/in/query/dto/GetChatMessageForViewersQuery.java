package com.umc.product.chat.application.port.in.query.dto;

import java.util.List;
import java.util.Objects;

public record GetChatMessageForViewersQuery(
    Long roomId,
    Long messageId,
    List<Long> viewerMemberIds
) {

    public GetChatMessageForViewersQuery {
        roomId = requirePositive(roomId, "roomId");
        messageId = requirePositive(messageId, "messageId");
        viewerMemberIds = Objects.requireNonNull(viewerMemberIds, "viewerMemberIds must not be null")
            .stream()
            .map(memberId -> requirePositive(memberId, "viewerMemberId"))
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
