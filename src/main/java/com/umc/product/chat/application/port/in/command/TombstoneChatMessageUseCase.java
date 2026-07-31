package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.TombstoneChatMessageCommand;

public interface TombstoneChatMessageUseCase {

    ChatMessageMutationResult tombstone(TombstoneChatMessageCommand command);
}
