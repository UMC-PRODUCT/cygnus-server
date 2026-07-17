package com.umc.product.chat.application.policy;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

/** Engine-native {@code chat.standalone} room의 최소 self-service 정책이다. */
@Component
public class ChatStandaloneRoomOwnerPolicy implements ChatRoomOwnerPolicy {

    @Override
    public String namespace() {
        return ChatRoomOwnerReference.STANDALONE_NAMESPACE;
    }

    @Override
    public boolean allows(
        ChatRoomOwnerReference ownerReference,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext
    ) {
        if (!isCanonicalStandalone(ownerReference)
            || operation == null
            || actorContext == null
            || !actorContext.hasAuthenticatedActor()) {
            return false;
        }

        return switch (operation) {
            case CREATE, READ, SEND -> true;
            case JOIN, LEAVE -> actorContext.isSelfTarget();
            case PIN, UNPIN, DELETE, MEMBERSHIP_MANAGE -> false;
        };
    }

    private boolean isCanonicalStandalone(ChatRoomOwnerReference ownerReference) {
        return ownerReference != null
            && namespace().equals(ownerReference.namespace())
            && ownerReference.roomId().toString().equals(ownerReference.ownerResourceKey())
            && ChatRoomOwnerReference.DEFAULT_SLOT.equals(ownerReference.slot());
    }
}
