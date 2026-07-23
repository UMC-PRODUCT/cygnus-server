package com.umc.product.chat.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.MessageContentType;

public record ChatMessageGraphQlResponse(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileIds,
    Instant createdAt,
    Long replyToMessageId
) {

    public static ChatMessageGraphQlResponse from(ChatMessageInfo info) {
        return info == null ? null : new ChatMessageGraphQlResponse(
            info.messageId(),
            info.roomId(),
            info.senderMemberId(),
            info.contentType(),
            info.content(),
            info.fileMetadataIds(),
            info.createdAt(),
            info.replyToMessageId()
        );
    }
}
