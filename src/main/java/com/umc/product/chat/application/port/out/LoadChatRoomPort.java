package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoom;
import java.util.List;

public interface LoadChatRoomPort {

    ChatRoom getById(Long roomId);

    List<ChatRoom> listByMemberId(Long memberId);
}
