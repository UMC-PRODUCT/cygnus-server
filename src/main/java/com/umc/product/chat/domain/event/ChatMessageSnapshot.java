package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;

public record ChatMessageSnapshot(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Long replyToMessageId,
    UUID clientMessageId,
    List<Long> mentionedMemberIds,
    Instant createdAt,
    Instant editedAt,
    Instant deletedAt
) {
    public ChatMessageSnapshot {
        fileMetadataIds = List.copyOf(fileMetadataIds);
        mentionedMemberIds = List.copyOf(mentionedMemberIds);
    }

    public static ChatMessageSnapshot from(ChatMessage message, List<Long> mentionedMemberIds) {
        return new ChatMessageSnapshot(
            message.getId(),
            message.getRoomId(),
            message.getSenderMemberId(),
            message.getContentType(),
            message.getContent(),
            message.getFileMetadataIds(),
            message.getReplyToMessageId(),
            message.getClientMessageId(),
            mentionedMemberIds,
            message.getCreatedAt(),
            message.getEditedAt(),
            message.getDeletedAt()
        );
    }
}
