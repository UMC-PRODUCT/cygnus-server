package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.application.port.in.command.dto.PinChatRoomMessageCommand;

public interface ManageChatRoomPinnedMessageUseCase {

    void pin(PinChatRoomMessageCommand command);

    void unpin(Long roomId);
}
