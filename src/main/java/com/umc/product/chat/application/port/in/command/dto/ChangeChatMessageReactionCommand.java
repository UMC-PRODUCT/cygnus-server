package com.umc.product.chat.application.port.in.command.dto;

public record ChangeChatMessageReactionCommand(
    Long roomId,
    Long messageId,
    Long memberId,
    String emoji
) {
}
