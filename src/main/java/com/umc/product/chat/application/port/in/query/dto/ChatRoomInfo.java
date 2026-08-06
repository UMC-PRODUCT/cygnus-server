package com.umc.product.chat.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

public record ChatRoomInfo(
    Long roomId,
    Instant createdAt,
    ChatMessageInfo pinnedMessage,
    List<Long> memberIds
) {
}
