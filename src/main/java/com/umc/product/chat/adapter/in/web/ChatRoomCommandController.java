package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import com.umc.product.chat.adapter.in.web.swagger.ChatRoomCommandApi;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
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
public class ChatRoomCommandController implements ChatRoomCommandApi {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final DeleteChatRoomUseCase deleteChatRoomUseCase;

    // NOTE: 이 엔드포인트는 클라이언트가 채팅방을 직접 생성하는 경우를 위한 것입니다.
    // 서버 내부 도메인은 이 API를 거치지 않고 CreateChatRoomUseCase를 직접 호출합니다.
    @PostMapping
    @Override
    public ChatRoomResponse create(@CurrentMember MemberPrincipal principal) {
        return ChatRoomResponse.from(createChatRoomUseCase.create(CreateChatRoomCommand.from(principal.getMemberId())));
    }

    // TODO: 채팅 기능 확정 시 삭제 권한 정책 결정 필요 (예: 방장만 삭제 가능 여부)
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void delete(@PathVariable Long roomId) {
        deleteChatRoomUseCase.delete(roomId);
    }
}
