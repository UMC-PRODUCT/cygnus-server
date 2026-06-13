package com.umc.product.chat.adapter.in.web.swagger;

import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Chat | 채팅방 Command", description = "채팅방 생성·삭제 API")
public interface ChatRoomCommandApi {

    @Operation(
        summary = "[CHAT-101] 채팅방 생성",
        description = """
            새 채팅방을 생성하고, 요청자(`@CurrentMember`)를 생성과 동시에 방 멤버로 등록합니다.

            응답으로 생성된 `roomId`, 생성 시각, 초기 멤버 목록을 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생성 성공")
    })
    ChatRoomResponse create(@CurrentMember MemberPrincipal principal);

    @Operation(
        summary = "[CHAT-102] 채팅방 삭제",
        description = """
            채팅방을 삭제합니다.

            > 현재는 별도 권한 검증 없이 삭제됩니다. 채팅 기능 정책이 확정되면
            > 방장/운영진만 삭제 가능하도록 권한 정책이 추가될 예정입니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공 (본문 없음)")
    })
    void delete(
        @Parameter(description = "삭제할 채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId
    );
}
