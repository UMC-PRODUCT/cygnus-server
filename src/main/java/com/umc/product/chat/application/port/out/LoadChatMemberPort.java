package com.umc.product.chat.application.port.out;

import java.util.List;

import com.umc.product.chat.domain.ChatMember;

public interface LoadChatMemberPort {

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    ChatMember getByRoomIdAndMemberId(Long roomId, Long memberId);

    List<ChatMember> listByRoomId(Long roomId);

    /**
     * 주어진 roomId 집합 중 해당 멤버가 실제 참여 중인 방의 id만 반환한다. 없으면 빈 목록
     */
    List<Long> listRoomIdsByMemberIdAndRoomIdIn(Long memberId, List<Long> roomIds);
}
