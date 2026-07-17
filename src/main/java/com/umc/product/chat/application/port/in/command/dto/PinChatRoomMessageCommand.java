package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public record PinChatRoomMessageCommand(
    ChatRoomOwnerReference expectedOwner,
    ChatRoomActorContext actorContext,
    Long messageId
) {
}
