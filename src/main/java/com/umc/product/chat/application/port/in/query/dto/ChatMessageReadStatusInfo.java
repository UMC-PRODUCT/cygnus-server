package com.umc.product.chat.application.port.in.query.dto;

public record ChatMessageReadStatusInfo(
    Long roomId,
    Long messageId,
    Long targetMemberId,
    boolean read
) {
}
