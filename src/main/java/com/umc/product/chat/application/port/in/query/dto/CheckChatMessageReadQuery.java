package com.umc.product.chat.application.port.in.query.dto;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public record CheckChatMessageReadQuery(
    ChatRoomOwnerReference expectedOwner,
    ChatRoomActorContext actorContext,
    Long messageId
) {
}
