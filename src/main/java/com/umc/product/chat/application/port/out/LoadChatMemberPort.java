package com.umc.product.chat.application.port.out;

import java.util.List;

import com.umc.product.chat.domain.ChatMember;

public interface LoadChatMemberPort {

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    ChatMember getByRoomIdAndMemberId(Long roomId, Long memberId);

    List<ChatMember> listByRoomId(Long roomId);
}
