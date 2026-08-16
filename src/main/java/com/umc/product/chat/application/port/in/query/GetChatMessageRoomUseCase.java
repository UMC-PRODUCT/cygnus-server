package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.GetChatMessageRoomQuery;

public interface GetChatMessageRoomUseCase {

    Long getRoomId(GetChatMessageRoomQuery query);
}
