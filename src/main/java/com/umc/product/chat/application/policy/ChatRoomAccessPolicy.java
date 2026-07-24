package com.umc.product.chat.application.policy;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.authorization.ChatPolicyAuthorizationService;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
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
    private final ChatPolicyAuthorizationService policyAuthorizationService;

    /**
     * 멤버가 해당 방의 참여자인지 검증한다. 참여자가 아니면 {@link ChatErrorCode#CHAT_ROOM_ACCESS_DENIED} 예외를 던진다.
     */
    public void verifyMember(Long roomId, Long memberId) {
        verifyMember(ChatPolicyAction.ROOM_READ, roomId, memberId);
    }

    public void verifyMember(ChatPolicyAction action, Long roomId, Long memberId) {
        if (!hasAccess(action, roomId, memberId)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    public boolean hasAccess(ChatPolicyAction action, Long roomId, Long memberId) {
        boolean roomMember = loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId);
        return policyAuthorizationService.evaluate(action, roomMember, false, false);
    }

    public void verifyMessageMutation(
        ChatPolicyAction action,
        Long roomId,
        Long memberId,
        boolean messageAuthor,
        boolean moderator
    ) {
        boolean roomMember = loadChatMemberPort.existsByRoomIdAndMemberId(roomId, memberId);
        if (!policyAuthorizationService.evaluate(
            action,
            roomMember,
            messageAuthor,
            moderator)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_MUTATION_FORBIDDEN);
        }
    }

    public void verifyAllMembers(
        ChatPolicyAction action,
        Long roomId,
        Collection<Long> memberIds
    ) {
        Set<Long> roomMemberIds = loadChatMemberPort.listByRoomId(roomId).stream()
            .map(ChatMember::getMemberId)
            .collect(Collectors.toUnmodifiableSet());
        boolean allRoomMembers = roomMemberIds.containsAll(memberIds);
        if (!policyAuthorizationService.evaluate(action, allRoomMembers, false, false)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    public boolean authorizeDerivedMembership(ChatPolicyAction action, boolean roomMember) {
        return policyAuthorizationService.evaluate(action, roomMember, false, false);
    }
}
