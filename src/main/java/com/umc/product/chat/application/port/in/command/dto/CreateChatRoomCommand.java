package com.umc.product.chat.application.port.in.command.dto;

public record CreateChatRoomCommand(
    Long creatorMemberId
) {
    public static CreateChatRoomCommand from(Long memberId) {
        return new CreateChatRoomCommand(memberId);
    }
}
