package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.ChatRoomReadScope;

public record CreateChatRoomCommand(
    Long creatorMemberId,
    ChatRoomReadScope readScope
) {
    /**
     * 멤버만 조회할 수 있는 방을 만든다. 조회 범위를 명시하지 않는 소비 도메인의 기본 진입점이다.
     */
    public static CreateChatRoomCommand from(Long memberId) {
        return new CreateChatRoomCommand(memberId, ChatRoomReadScope.MEMBER_ONLY);
    }

    public static CreateChatRoomCommand of(Long memberId, ChatRoomReadScope readScope) {
        return new CreateChatRoomCommand(memberId, readScope);
    }
}
