package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatMessage;

public interface SaveChatMessagePort {

    ChatMessage save(ChatMessage chatMessage);
}
