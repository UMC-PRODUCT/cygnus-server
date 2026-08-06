package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;

public interface CreateChatMessageUseCase {

    ChatMessageMutationResult create(CreateChatMessageCommand command);
}
