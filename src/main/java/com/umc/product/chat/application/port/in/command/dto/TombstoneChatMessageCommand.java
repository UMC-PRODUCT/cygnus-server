package com.umc.product.chat.application.port.in.command.dto;

public record TombstoneChatMessageCommand(
    Long roomId,
    Long messageId,
    Long requesterMemberId,
    boolean moderator
) {
}
