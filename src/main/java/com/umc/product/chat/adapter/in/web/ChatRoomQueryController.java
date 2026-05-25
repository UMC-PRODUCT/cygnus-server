package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "Chat | 채팅방 Query", description = "채팅방 조회")
public class ChatRoomQueryController {

    private final GetChatRoomUseCase getChatRoomUseCase;

    @GetMapping("/{roomId}")
    @Operation(summary = "[CHAT-001] 채팅방 조회", description = "채팅방 정보와 멤버 목록을 조회합니다.")
    public ChatRoomResponse getById(@PathVariable Long roomId) {
        return ChatRoomResponse.from(getChatRoomUseCase.getById(roomId));
    }
}
