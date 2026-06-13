package com.umc.product.chat.adapter.in.web.swagger;

import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Chat | 채팅방 Query", description = "채팅방 단건 조회 API")
public interface ChatRoomQueryApi {

    @Operation(
        summary = "[CHAT-001] 채팅방 조회",
        description = """
            채팅방의 기본 정보와 현재 참여 중인 멤버 목록을 조회합니다.

            - 응답의 `memberIds`는 방에 속한 멤버의 `memberId` 목록입니다.
            - 메시지 내역 자체는 포함되지 않습니다. 메시지는 `[CHAT-002] 메시지 내역 조회`를 사용하세요.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음 (CHAT-0001)")
    })
    ChatRoomResponse getById(
        @Parameter(description = "조회할 채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId
    );
}
