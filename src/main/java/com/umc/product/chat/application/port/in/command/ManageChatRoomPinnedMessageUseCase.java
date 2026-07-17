package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.PinChatRoomMessageCommand;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

public interface ManageChatRoomPinnedMessageUseCase {

    void pin(PinChatRoomMessageCommand command);

    void unpin(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext);
}
