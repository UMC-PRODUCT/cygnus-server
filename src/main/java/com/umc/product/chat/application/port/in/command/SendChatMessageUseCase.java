package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;

public interface SendChatMessageUseCase {

    ChatMessageInfo send(SendChatMessageCommand command);
}
