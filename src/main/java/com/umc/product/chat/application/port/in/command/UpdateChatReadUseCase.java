package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.ChatReadMutationResult;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;

public interface UpdateChatReadUseCase {

    ChatReadMutationResult update(UpdateChatReadCommand command);
}
