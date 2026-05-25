package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatMember;
import java.util.List;

public interface LoadChatMemberPort {

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    List<ChatMember> listByRoomId(Long roomId);
}
