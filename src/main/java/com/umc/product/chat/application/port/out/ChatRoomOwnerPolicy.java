package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

/** Chat consumer가 자기 namespace의 business permission을 평가하는 SPI다. */
public interface ChatRoomOwnerPolicy {

    String namespace();

    boolean allows(
        ChatRoomOwnerReference ownerReference,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext
    );
}
