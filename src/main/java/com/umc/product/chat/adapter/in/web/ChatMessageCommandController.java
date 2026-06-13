package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.application.port.in.command.MarkChatRoomReadUseCase;
import com.umc.product.chat.adapter.in.web.swagger.ChatMessageCommandApi;
import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatMessageCommandController implements ChatMessageCommandApi {

    private final MarkChatRoomReadUseCase markChatRoomReadUseCase;

    @PatchMapping("/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void markRead(
        @PathVariable Long roomId,
        @CurrentMember MemberPrincipal principal
    ) {
        markChatRoomReadUseCase.markRead(MarkChatRoomReadCommand.of(roomId, principal.getMemberId()));
    }
}
