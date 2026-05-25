package com.umc.product.chat.adapter.in.web.dto.request;

import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import jakarta.validation.constraints.NotNull;

public record JoinChatRoomRequest(
    @NotNull(message = "멤버 ID는 필수입니다.")
    Long memberId
) {
    public JoinChatRoomCommand toCommand(Long roomId) {
        return new JoinChatRoomCommand(roomId, memberId);
    }
}
