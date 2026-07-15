package com.umc.product.chat.application.port.in.query.dto;

public record CheckChatMessageReadQuery(
    Long roomId,
    Long messageId,
    Long requesterMemberId,
    Long targetMemberId
) {
}
