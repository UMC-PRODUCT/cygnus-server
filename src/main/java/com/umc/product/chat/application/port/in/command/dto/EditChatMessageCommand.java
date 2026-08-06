package com.umc.product.chat.application.port.in.command.dto;

public record EditChatMessageCommand(
    Long roomId,
    Long messageId,
    Long editorMemberId,
    String content
) {
}
