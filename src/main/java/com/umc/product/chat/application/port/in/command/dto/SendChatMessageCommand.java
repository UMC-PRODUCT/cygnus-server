package com.umc.product.chat.application.port.in.command.dto;

import java.util.List;

import com.umc.product.chat.domain.MessageContentType;

public record SendChatMessageCommand(
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Long replyToMessageId
) {

    public SendChatMessageCommand(
        Long roomId,
        Long senderMemberId,
        MessageContentType contentType,
        String content,
        List<String> fileMetadataIds
    ) {
        this(roomId, senderMemberId, contentType, content, fileMetadataIds, null);
    }
}
