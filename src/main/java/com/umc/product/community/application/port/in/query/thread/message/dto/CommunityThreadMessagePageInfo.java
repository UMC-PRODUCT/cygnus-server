package com.umc.product.community.application.port.in.query.thread.message.dto;

import java.util.List;

public record CommunityThreadMessagePageInfo(
    List<CommunityThreadMessageInfo> messages,
    boolean hasMore,
    Long nextBefore
) {

    public CommunityThreadMessagePageInfo {
        messages = messages == null ? List.of() : List.copyOf(messages);
        nextBefore = nextBefore == null ? null : requirePositive(nextBefore, "nextBefore");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
