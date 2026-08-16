package com.umc.product.chat.application.port.in.query.dto;

public record ChatMessageReplyInfo(
    Long messageId,
    Long senderMemberId,
    String snippet
) {
}
