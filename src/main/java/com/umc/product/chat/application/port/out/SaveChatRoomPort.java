package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoom;

public interface SaveChatRoomPort {

    ChatRoom save(ChatRoom chatRoom);

    void delete(ChatRoom chatRoom);
}
