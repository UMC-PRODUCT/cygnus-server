package com.umc.product.chat.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;

public record ChatRoomResponse(
    Long roomId,
    Instant createdAt,
    List<Long> memberIds
) {
    public static ChatRoomResponse from(ChatRoomInfo info) {
        return new ChatRoomResponse(info.roomId(), info.createdAt(), info.memberIds());
    }
}
