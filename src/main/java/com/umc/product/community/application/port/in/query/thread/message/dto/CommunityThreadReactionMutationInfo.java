package com.umc.product.community.application.port.in.query.thread.message.dto;

import java.util.List;

public record CommunityThreadReactionMutationInfo(
    Long messageId,
    List<CommunityThreadReactionInfo> reactions,
    boolean deduplicated
) {

    public CommunityThreadReactionMutationInfo {
        messageId = requirePositive(messageId, "messageId");
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
