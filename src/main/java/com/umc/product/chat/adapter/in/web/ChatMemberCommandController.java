package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.adapter.in.web.dto.request.JoinChatRoomRequest;
import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "Chat | 채팅방 멤버 Command", description = "채팅방 멤버 추가, 제거")
public class ChatMemberCommandController {

    private final JoinChatRoomUseCase joinChatRoomUseCase;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;

    // TODO: 채팅 기능 확정 시 멤버 추가 권한 정책 결정 필요 (예: 방장/운영진만 추가 가능 여부)
    @PostMapping("/{roomId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[CHAT-201] 채팅방 멤버 추가", description = "채팅방에 멤버를 추가합니다.")
    public void join(
        @PathVariable Long roomId,
        @Valid @RequestBody JoinChatRoomRequest request
    ) {
        joinChatRoomUseCase.joinChatRoom(request.toCommand(roomId));
    }

    // TODO: 채팅 기능 확정 시 멤버 제거 권한 정책 결정 필요 (예: 본인만 퇴장 가능 여부, 강퇴 기능 여부)
    @DeleteMapping("/{roomId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[CHAT-202] 채팅방 멤버 제거", description = "채팅방에서 멤버를 제거합니다.")
    public void leave(
        @PathVariable Long roomId,
        @PathVariable Long memberId
    ) {
        leaveChatRoomUseCase.leaveChatRoom(LeaveChatRoomCommand.of(roomId, memberId));
    }
}
