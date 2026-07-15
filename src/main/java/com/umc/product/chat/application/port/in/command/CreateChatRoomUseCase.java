package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;

public interface CreateChatRoomUseCase {

    ChatRoomInfo create(CreateChatRoomCommand command);
}
