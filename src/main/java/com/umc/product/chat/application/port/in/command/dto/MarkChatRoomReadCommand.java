package com.umc.product.chat.application.port.in.command.dto;

public record MarkChatRoomReadCommand(
    Long roomId,
    Long memberId,
    Long lastSeenMessageId
) {
    public static MarkChatRoomReadCommand of(Long roomId, Long memberId, Long lastSeenMessageId) {
        return new MarkChatRoomReadCommand(roomId, memberId, lastSeenMessageId);
    }
}
