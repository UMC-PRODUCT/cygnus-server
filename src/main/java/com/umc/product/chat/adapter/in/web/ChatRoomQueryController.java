package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import com.umc.product.chat.adapter.in.web.swagger.ChatRoomQueryApi;
import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomQueryController implements ChatRoomQueryApi {

    private final GetChatRoomUseCase getChatRoomUseCase;

    @GetMapping("/{roomId}")
    @Override
    public ChatRoomResponse getById(@PathVariable Long roomId) {
        return ChatRoomResponse.from(getChatRoomUseCase.getById(roomId));
    }
}
