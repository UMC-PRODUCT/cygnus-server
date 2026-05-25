package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatMember;

public interface SaveChatMemberPort {

    ChatMember save(ChatMember chatMember);

    void delete(Long roomId, Long memberId);
}
