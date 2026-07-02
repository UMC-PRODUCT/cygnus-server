package com.umc.product.chat.application.port.out;

import java.util.List;

import com.umc.product.chat.domain.ChatRoom;

public interface LoadChatRoomPort {

    ChatRoom getById(Long roomId);

    List<ChatRoom> listByMemberId(Long memberId);
}
