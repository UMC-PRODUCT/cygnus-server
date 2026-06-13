package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.adapter.in.web.dto.request.JoinChatRoomRequest;
import com.umc.product.chat.adapter.in.web.swagger.ChatMemberCommandApi;
import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
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
public class ChatMemberCommandController implements ChatMemberCommandApi {

    private final JoinChatRoomUseCase joinChatRoomUseCase;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;

    // TODO: 채팅 기능 확정 시 멤버 추가 권한 정책 결정 필요 (예: 방장/운영진만 추가 가능 여부)
    @PostMapping("/{roomId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void join(
        @PathVariable Long roomId,
        @Valid @RequestBody JoinChatRoomRequest request
    ) {
        joinChatRoomUseCase.joinChatRoom(request.toCommand(roomId));
    }

    // TODO: 채팅 기능 확정 시 멤버 제거 권한 정책 결정 필요 (예: 본인만 퇴장 가능 여부, 강퇴 기능 여부)
    @DeleteMapping("/{roomId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void leave(
        @PathVariable Long roomId,
        @PathVariable Long memberId
    ) {
        leaveChatRoomUseCase.leaveChatRoom(LeaveChatRoomCommand.of(roomId, memberId));
    }
}
