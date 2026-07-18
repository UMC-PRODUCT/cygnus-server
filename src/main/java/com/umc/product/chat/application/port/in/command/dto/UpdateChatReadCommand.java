package com.umc.product.chat.application.port.in.command.dto;

public record UpdateChatReadCommand(
    Long roomId,
    Long memberId,
    Long lastReadMessageId
) {
}
