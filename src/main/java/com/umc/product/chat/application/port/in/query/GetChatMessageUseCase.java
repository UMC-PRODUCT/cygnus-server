package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;

public interface GetChatMessageUseCase {

    ChatMessageInfo getMessage(GetChatMessageQuery query);
}
