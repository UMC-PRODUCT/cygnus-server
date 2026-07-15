package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;

public interface MarkChatRoomReadUseCase {

    void markRead(MarkChatRoomReadCommand command);
}
