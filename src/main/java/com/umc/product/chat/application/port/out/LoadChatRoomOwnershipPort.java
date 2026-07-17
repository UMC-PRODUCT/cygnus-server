package com.umc.product.chat.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.chat.domain.ChatRoomOwnerReference;

/** ChatRoom ownership registry 조회 Port. */
public interface LoadChatRoomOwnershipPort {

    Optional<ChatRoomOwnerReference> findByRoomId(Long roomId);

    Optional<ChatRoomOwnerReference> findByRoomIdForUpdate(Long roomId);

    Optional<ChatRoomOwnerReference> findByOwner(String namespace, String ownerResourceKey, String slot);

    List<String> findDistinctNamespaces();

    default Optional<ChatRoomOwnerReference> findForUpdate(Long roomId) {
        return findByRoomIdForUpdate(roomId);
    }

    default List<String> listDistinctNamespaces() {
        return findDistinctNamespaces();
    }
}
