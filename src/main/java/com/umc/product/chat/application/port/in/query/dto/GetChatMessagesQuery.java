package com.umc.product.chat.application.port.in.query.dto;

public record GetChatMessagesQuery(
    Long roomId,
    Long memberId,
    Long cursorId,
    int size
) {
}
