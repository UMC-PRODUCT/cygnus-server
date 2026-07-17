package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public record MarkChatRoomReadCommand(
    ChatRoomOwnerReference expectedOwner,
    ChatRoomActorContext actorContext,
    Long lastSeenMessageId
) {
    public static MarkChatRoomReadCommand of(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomActorContext actorContext,
        Long lastSeenMessageId
    ) {
        return new MarkChatRoomReadCommand(expectedOwner, actorContext, lastSeenMessageId);
    }
}
