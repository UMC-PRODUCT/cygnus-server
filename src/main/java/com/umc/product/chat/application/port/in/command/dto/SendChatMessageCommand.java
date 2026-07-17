package com.umc.product.chat.application.port.in.command.dto;

import java.util.List;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.MessageContentType;

public record SendChatMessageCommand(
    ChatRoomOwnerReference expectedOwner,
    ChatRoomActorContext actorContext,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Long replyToMessageId
) {

    public SendChatMessageCommand(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomActorContext actorContext,
        MessageContentType contentType,
        String content,
        List<String> fileMetadataIds
    ) {
        this(expectedOwner, actorContext, contentType, content, fileMetadataIds, null);
    }
}
