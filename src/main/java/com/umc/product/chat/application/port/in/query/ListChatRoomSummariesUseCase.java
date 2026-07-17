package com.umc.product.chat.application.port.in.query;

import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

/**
 * 소비 도메인이 server-generate한 expected owner 집합에 대해 방 요약(마지막 메시지 + 안 읽은 수)을 조회한다.
 * <p>
 * 엔진은 "이 멤버의 모든 방"을 스스로 열거하지 않는다. 각 expected owner는 ownership registry와 policy,
 * Chat membership을 모두 통과해야 하며, raw room ID만 받는 fallback은 제공하지 않는다.
 *
 * @param actorContext 서버 인증에서 resolve한 조회 actor
 * @param expectedOwners 소비 도메인이 server-generate한 ownership 좌표. raw room ID나 namespace 입력을 받지 않는다.
 */
public interface ListChatRoomSummariesUseCase {

    List<ChatRoomSummaryInfo> listRoomSummaries(
        ChatRoomActorContext actorContext,
        List<ChatRoomOwnerReference> expectedOwners
    );
}
