package com.umc.product.chat.application.port.in.command.dto;

public record JoinChatRoomCommand(
    Long roomId,
    Long memberId
) {
}
