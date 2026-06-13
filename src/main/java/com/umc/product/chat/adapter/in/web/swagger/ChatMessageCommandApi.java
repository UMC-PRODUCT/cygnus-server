package com.umc.product.chat.adapter.in.web.swagger;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Chat | 채팅 메시지 Command", description = "채팅방 읽음 처리 API")
public interface ChatMessageCommandApi {

    @Operation(
        summary = "[CHAT-103] 읽음 처리",
        description = """
            방을 현재 최신 메시지까지 읽음 처리합니다. 요청 본문은 없습니다.

            **서버 주도 방식**
            - 읽음 위치는 클라이언트가 지정하지 않고, 서버가 조회한 방의 최신 메시지 id를 기준으로 갱신합니다.
              따라서 클라이언트가 임의의 메시지 id로 읽음 위치를 조작할 수 없습니다.
            - 읽음 위치는 뒤로 되돌아가지 않습니다(이미 더 최신을 읽은 상태면 변화 없음).
            - 메시지가 아직 없는 방이면 아무 변화 없이 정상 종료합니다.

            처리 후 해당 방의 `unreadCount`는 0이 됩니다(`[CHAT-003] 내 채팅방 목록 조회` 기준).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "읽음 처리 성공 (본문 없음)"),
        @ApiResponse(responseCode = "404", description = "요청자가 해당 방의 멤버가 아님 (CHAT-0003)")
    })
    void markRead(
        @Parameter(description = "읽음 처리할 채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId,
        @CurrentMember MemberPrincipal principal
    );
}
