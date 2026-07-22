package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.EditChatMessageCommand;

public interface EditChatMessageUseCase {

    ChatMessageMutationResult edit(EditChatMessageCommand command);
}
