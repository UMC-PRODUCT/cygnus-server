package com.umc.product.chat.adapter.in.web.swagger;

import com.umc.product.chat.adapter.in.web.dto.request.JoinChatRoomRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Chat | 채팅방 멤버 Command", description = "채팅방 멤버 추가·제거 API")
public interface ChatMemberCommandApi {

    @Operation(
        summary = "[CHAT-201] 채팅방 멤버 추가",
        description = """
            채팅방에 멤버를 추가합니다. 요청 본문의 `memberId`가 추가 대상입니다.

            - 이미 방에 참여 중인 멤버를 다시 추가하면 충돌 오류(CHAT-0002)가 발생합니다.

            > 현재는 별도 권한 검증 없이 추가됩니다. 정책 확정 시 방장/운영진만 추가 가능하도록 제한될 예정입니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "추가 성공 (본문 없음)"),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음 (CHAT-0001)"),
        @ApiResponse(responseCode = "409", description = "이미 참여 중인 멤버 (CHAT-0002)")
    })
    void join(
        @Parameter(description = "멤버를 추가할 채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId,
        @Valid @RequestBody JoinChatRoomRequest request
    );

    @Operation(
        summary = "[CHAT-202] 채팅방 멤버 제거",
        description = """
            채팅방에서 특정 멤버를 제거(퇴장)합니다.

            > 현재는 별도 권한 검증 없이 제거됩니다. 정책 확정 시 본인 퇴장만 허용할지,
            > 운영진 강퇴 기능을 둘지 등이 결정될 예정입니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "제거 성공 (본문 없음)"),
        @ApiResponse(responseCode = "404", description = "채팅방 또는 멤버를 찾을 수 없음 (CHAT-0001 / CHAT-0003)")
    })
    void leave(
        @Parameter(description = "채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId,
        @Parameter(description = "제거할 멤버 ID", required = true, example = "10")
        @PathVariable Long memberId
    );
}
