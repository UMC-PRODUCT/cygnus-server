package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoom;

public interface LoadChatRoomPort {

    ChatRoom getById(Long roomId);
}
