package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public interface CheckChatRoomAccessUseCase {

    boolean hasChatRoomAccess(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext);
}
