package com.umc.product.chat.application.port.in.command.dto;

public record MarkChatRoomReadCommand(
    Long roomId,
    Long memberId
) {
    public static MarkChatRoomReadCommand of(Long roomId, Long memberId) {
        return new MarkChatRoomReadCommand(roomId, memberId);
    }
}
