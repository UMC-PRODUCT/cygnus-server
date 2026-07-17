package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.ChatRoomActorContext;

/** Server-authenticated actor가 engine-native standalone room을 생성하는 command다. */
public record CreateChatRoomCommand(
    ChatRoomActorContext actorContext
) {
}
