package com.umc.product.chat.application.port.in.query;

import java.util.Map;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;

public interface GetChatMessageForViewersUseCase {

    Map<Long, ChatMessageInfo> getMessageForViewers(GetChatMessageForViewersQuery query);
}
