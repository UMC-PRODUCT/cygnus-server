package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;

public interface ManageChatMessageReactionUseCase {

    ChatReactionMutationResult add(ChangeChatMessageReactionCommand command);

    ChatReactionMutationResult remove(ChangeChatMessageReactionCommand command);
}
