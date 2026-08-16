package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadMessageReplyInfo(
    Long messageId,
    String senderName,
    String snippet
) {

    public CommunityThreadMessageReplyInfo {
        messageId = requirePositive(messageId, "messageId");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
