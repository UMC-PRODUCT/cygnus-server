package com.umc.product.chat.application.policy;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

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
    private final LoadChatRoomPort loadChatRoomPort;

    /**
     * 멤버가 해당 방의 참여자인지 검증한다. 참여자가 아니면 {@link ChatErrorCode#CHAT_ROOM_ACCESS_DENIED} 예외를 던진다.
     * <p>
     * 메시지 생성/수정/삭제, 리액션, 읽음 갱신 등 모든 변경은 방의 조회 범위와 무관하게 이 검증을 사용한다.
     */
    public void verifyMember(Long roomId, Long memberId) {
        if (!loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    /**
     * 멤버가 해당 방의 메시지를 조회할 수 있는지 검증한다. 참여자이거나 방이 공개 조회 범위여야 한다.
     * <p>
     * 참여자 여부를 먼저 확인해 {@code MEMBER_ONLY} 방의 조회는 기존과 같이 쿼리 한 번으로 끝낸다.
     * 방을 찾지 못하면 존재 여부를 노출하지 않도록 {@link ChatErrorCode#CHAT_ROOM_ACCESS_DENIED}로 막는다.
     */
    public void verifyReadable(Long roomId, Long memberId) {
        if (loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            return;
        }
        boolean publiclyReadable = loadChatRoomPort.findById(roomId)
            .map(ChatRoom::isPubliclyReadable)
            .orElse(false);
        if (!publiclyReadable) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
