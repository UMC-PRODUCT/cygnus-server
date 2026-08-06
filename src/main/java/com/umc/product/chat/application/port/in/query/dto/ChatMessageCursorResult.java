package com.umc.product.chat.application.port.in.query.dto;

import java.util.List;

public record ChatMessageCursorResult(
    List<ChatMessageInfo> content,
    Long nextCursor,
    boolean hasNext
) {
}
