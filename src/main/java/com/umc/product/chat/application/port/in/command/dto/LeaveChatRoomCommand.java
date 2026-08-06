package com.umc.product.chat.application.port.in.command.dto;

public record LeaveChatRoomCommand(
    Long roomId,
    Long memberId
) {
    public static LeaveChatRoomCommand of(Long roomId, Long memberId) {
        return new LeaveChatRoomCommand(roomId, memberId);
    }
}
