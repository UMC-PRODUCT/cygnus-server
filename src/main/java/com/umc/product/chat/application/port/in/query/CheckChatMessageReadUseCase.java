package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageReadStatusInfo;
import com.umc.product.chat.application.port.in.query.dto.CheckChatMessageReadQuery;

public interface CheckChatMessageReadUseCase {

    ChatMessageReadStatusInfo checkRead(CheckChatMessageReadQuery query);
}
