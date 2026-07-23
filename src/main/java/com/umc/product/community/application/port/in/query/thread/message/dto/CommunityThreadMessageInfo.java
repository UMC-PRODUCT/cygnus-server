package com.umc.product.community.application.port.in.query.thread.message.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommunityThreadMessageInfo(
    Long messageId,
    Long threadId,
    Long senderId,
    String senderName,
    String content,
    CommunityThreadMessageType type,
    CommunityThreadMessageStatus status,
    List<String> fileMetadataIds,
    List<CommunityThreadMessageMentionInfo> mentions,
    CommunityThreadMessageReplyInfo replyTo,
    List<CommunityThreadReactionInfo> reactions,
    UUID clientMessageId,
    Instant createdAt,
    Instant editedAt,
    Instant deletedAt
) {

    public CommunityThreadMessageInfo {
        messageId = requirePositive(messageId, "messageId");
        threadId = requirePositive(threadId, "threadId");
        senderId = requirePositive(senderId, "senderId");
        fileMetadataIds = fileMetadataIds == null ? List.of() : List.copyOf(fileMetadataIds);
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
