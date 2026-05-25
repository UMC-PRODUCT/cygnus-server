package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;

public interface GetChatRoomUseCase {

    ChatRoomInfo getById(Long roomId);
}
