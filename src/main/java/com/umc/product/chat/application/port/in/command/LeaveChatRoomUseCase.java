package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;

public interface LeaveChatRoomUseCase {

    void leaveChatRoom(LeaveChatRoomCommand command);
}
