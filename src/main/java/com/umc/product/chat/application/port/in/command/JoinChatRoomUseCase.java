package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;

public interface JoinChatRoomUseCase {

    void joinChatRoom(JoinChatRoomCommand command);
}
