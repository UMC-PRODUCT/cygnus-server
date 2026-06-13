package com.umc.product.chat.adapter.in.web;

import com.umc.product.chat.adapter.in.web.dto.response.ChatMessageResponse;
import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomSummaryResponse;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.GetMyChatRoomsUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.chat.adapter.in.web.swagger.ChatMessageQueryApi;
import com.umc.product.global.security.annotation.CurrentMember;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Validated
public class ChatMessageQueryController implements ChatMessageQueryApi {

    private final GetChatMessagesUseCase getChatMessagesUseCase;
    private final GetMyChatRoomsUseCase getMyChatRoomsUseCase;

    @GetMapping("/{roomId}/messages")
    @Override
    public CursorResponse<ChatMessageResponse> getMessages(
        @PathVariable @Positive Long roomId,
        @CurrentMember MemberPrincipal principal,
        @RequestParam(required = false) @Positive Long cursor,
        @RequestParam(defaultValue = "30") @Min(1) int size
    ) {
        ChatMessageCursorResult result = getChatMessagesUseCase.getMessages(
            new GetChatMessagesQuery(roomId, principal.getMemberId(), cursor, size));

        return CursorResponse.of(
            result.content().stream().map(ChatMessageResponse::from).toList(),
            result.nextCursor(),
            result.hasNext()
        );
    }

    @GetMapping
    @Override
    public List<ChatRoomSummaryResponse> getMyChatRooms(@CurrentMember MemberPrincipal principal) {
        return getMyChatRoomsUseCase.getMyChatRooms(principal.getMemberId()).stream()
            .map(ChatRoomSummaryResponse::from)
            .toList();
    }
}
