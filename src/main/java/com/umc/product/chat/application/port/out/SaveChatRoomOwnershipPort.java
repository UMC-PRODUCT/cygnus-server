package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.ChatRoomOwnership;

/** ChatRoom ownership registry 저장 Port. */
public interface SaveChatRoomOwnershipPort {

    ChatRoomOwnerReference save(ChatRoomOwnerReference reference);

    default ChatRoomOwnerReference save(ChatRoomOwnership ownership) {
        return save(ownership.toReference());
    }

    default ChatRoomOwnerReference register(ChatRoomOwnerReference reference) {
        return save(reference);
    }
}
