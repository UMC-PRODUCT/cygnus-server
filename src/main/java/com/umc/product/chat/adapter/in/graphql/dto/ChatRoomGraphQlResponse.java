package com.umc.product.chat.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;

public final class ChatRoomGraphQlResponse {

    private ChatRoomGraphQlResponse() {
    }

    public record Room(
        Long roomId,
        Instant createdAt,
        ChatMessageGraphQlResponse pinnedMessage,
        List<Long> memberIds
    ) {

        public static Room from(ChatRoomInfo info) {
            return new Room(
                info.roomId(),
                info.createdAt(),
                ChatMessageGraphQlResponse.from(info.pinnedMessage()),
                info.memberIds()
            );
        }
    }

    public record Summary(
        Long roomId,
        ChatMessageGraphQlResponse lastMessage,
        long unreadCount
    ) {

        public static Summary from(ChatRoomSummaryInfo info) {
            return new Summary(
                info.roomId(),
                ChatMessageGraphQlResponse.from(info.lastMessage()),
                info.unreadCount()
            );
        }
    }

    public record Messages(
        List<ChatMessageGraphQlResponse> content,
        Long nextCursor,
        boolean hasNext
    ) {
    }
}
