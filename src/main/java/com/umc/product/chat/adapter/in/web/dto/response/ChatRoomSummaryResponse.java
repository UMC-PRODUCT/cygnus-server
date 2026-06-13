package com.umc.product.chat.adapter.in.web.dto.response;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;

public record ChatRoomSummaryResponse(
    Long roomId,
    ChatMessageResponse lastMessage,
    long unreadCount
) {
    public static ChatRoomSummaryResponse from(ChatRoomSummaryInfo info) {
        return new ChatRoomSummaryResponse(
            info.roomId(),
            info.lastMessage() != null ? ChatMessageResponse.from(info.lastMessage()) : null,
            info.unreadCount()
        );
    }
}
