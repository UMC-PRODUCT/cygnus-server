package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public interface DeleteChatRoomUseCase {

    void delete(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext);
}
