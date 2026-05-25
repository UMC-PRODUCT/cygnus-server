package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "Chat | 채팅방 Command", description = "채팅방 생성, 삭제")
public class ChatRoomCommandController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final DeleteChatRoomUseCase deleteChatRoomUseCase;

    // NOTE: 이 엔드포인트는 클라이언트가 채팅방을 직접 생성하는 경우를 위한 것입니다.
    // Inquiry 등 내부 도메인은 이 API를 거치지 않고 CreateChatRoomUseCase를 직접 호출합니다.
    @PostMapping
    @Operation(summary = "[CHAT-101] 채팅방 생성", description = "새 채팅방을 생성합니다. 생성자는 자동으로 채팅방 멤버에 추가됩니다.")
    public ChatRoomResponse create(@CurrentMember MemberPrincipal principal) {
        return ChatRoomResponse.from(createChatRoomUseCase.create(CreateChatRoomCommand.from(principal.getMemberId())));
    }

    // TODO: 채팅 기능 확정 시 삭제 권한 정책 결정 필요 (예: 방장만 삭제 가능 여부)
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[CHAT-102] 채팅방 삭제", description = "채팅방을 삭제합니다.")
    public void delete(@PathVariable Long roomId) {
        deleteChatRoomUseCase.delete(roomId);
    }
}
