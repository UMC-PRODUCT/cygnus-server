package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public interface GetChatRoomUseCase {

    ChatRoomInfo getById(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext);
}
