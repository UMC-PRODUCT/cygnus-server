package com.umc.product.chat.application.policy;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 채팅방 접근 정책.
 * <p>
 * 멤버십 기반 인가를 한 곳에서 관리하여 command/query 서비스가 동일한 규칙을 공유하도록 한다.
 * 별도 권한 인터셉터(STOMP/REST 공통)가 도입되기 전까지 방 단위 접근 검증의 단일 진입점 역할을 한다.
 */
@Component
@RequiredArgsConstructor
public class ChatRoomAccessPolicy {

    private final LoadChatMemberPort loadChatMemberPort;

    /**
     * 멤버가 해당 방의 참여자인지 검증한다. 참여자가 아니면 {@link ChatErrorCode#CHAT_ROOM_ACCESS_DENIED} 예외를 던진다.
     */
    public void verifyMember(Long roomId, Long memberId) {
        if (!loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
