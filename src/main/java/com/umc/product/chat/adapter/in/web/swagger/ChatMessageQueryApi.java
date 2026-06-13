package com.umc.product.chat.adapter.in.web.swagger;

import com.umc.product.chat.adapter.in.web.dto.response.ChatMessageResponse;
import com.umc.product.chat.adapter.in.web.dto.response.ChatRoomSummaryResponse;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Chat | 채팅 메시지 Query", description = "채팅 메시지 내역 및 내 채팅방 목록 조회 API")
public interface ChatMessageQueryApi {

    @Operation(
        summary = "[CHAT-002] 메시지 내역 조회",
        description = """
            방의 메시지를 최신순(id 내림차순)으로 커서 페이지네이션 조회합니다.

            **접근 제어**
            - 요청자(`@CurrentMember`)가 해당 방의 멤버가 아니면 403(CHAT-0007)을 반환합니다.
              즉 `roomId`만 알아도 비참여자는 내역을 조회할 수 없습니다.

            **커서 사용법**
            - `cursor`를 비우면 가장 최신 메시지부터 조회합니다.
            - 응답의 `nextCursor`를 다음 요청의 `cursor`로 전달하면 그보다 더 과거 메시지를 이어서 조회합니다.
            - `hasNext`가 `false`이면 더 이상 과거 메시지가 없습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "size가 1 미만이거나 roomId/cursor가 양수가 아님"),
        @ApiResponse(responseCode = "403", description = "방 멤버가 아니어서 접근 불가 (CHAT-0007)")
    })
    CursorResponse<ChatMessageResponse> getMessages(
        @Parameter(description = "조회할 채팅방 ID", required = true, example = "1")
        @PathVariable Long roomId,

        @CurrentMember MemberPrincipal principal,

        @Parameter(description = "커서. 이 메시지 id보다 과거(작은 id) 메시지를 조회. 비우면 최신부터.", example = "1024")
        @RequestParam(required = false) Long cursor,

        @Parameter(description = "조회 개수(최소 1). 기본값 30.", example = "30")
        @RequestParam(defaultValue = "30") int size
    );

    @Operation(
        summary = "[CHAT-003] 내 채팅방 목록 조회",
        description = """
            요청자(`@CurrentMember`)가 속한 채팅방 목록을, 각 방의 마지막 메시지 미리보기와
            안 읽은 메시지 수와 함께 조회합니다.

            - 마지막 메시지 최신순으로 정렬되며, 메시지가 없는 방은 뒤로 정렬됩니다.
            - 각 방의 `lastMessage`는 메시지가 한 건도 없으면 `null`입니다.
            - `unreadCount`는 본인이 마지막으로 읽은 위치 이후 들어온, 본인이 보내지 않은 메시지 수입니다
              (시스템 메시지 포함).
            - 방 개수와 무관하게 고정된 횟수의 배치 쿼리로 처리되어 N+1이 발생하지 않습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공 (속한 방이 없으면 빈 배열)")
    })
    List<ChatRoomSummaryResponse> getMyChatRooms(@CurrentMember MemberPrincipal principal);
}
