package com.umc.product.chat.application.port.in.query.dto;

public record GetChatMessageQuery(
    Long roomId,
    Long memberId,
    Long messageId
) {
}
