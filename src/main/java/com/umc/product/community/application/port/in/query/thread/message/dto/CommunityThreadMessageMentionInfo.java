package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadMessageMentionInfo(
    Long memberId,
    String name
) {

    public CommunityThreadMessageMentionInfo {
        memberId = requirePositive(memberId, "memberId");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
