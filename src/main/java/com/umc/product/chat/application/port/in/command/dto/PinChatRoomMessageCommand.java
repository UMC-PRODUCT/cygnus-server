package com.umc.product.chat.application.port.in.command.dto;

public record PinChatRoomMessageCommand(
    Long roomId,
    Long messageId
) {
}
