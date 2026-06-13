package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;

public interface GetChatMessagesUseCase {

    ChatMessageCursorResult getMessages(GetChatMessagesQuery query);
}
