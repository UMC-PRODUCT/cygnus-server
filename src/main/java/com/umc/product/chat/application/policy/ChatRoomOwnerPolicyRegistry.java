package com.umc.product.chat.application.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

/** Namespace마다 정확히 하나의 Chat owner policy만 선택하는 fail-closed registry다. */
@Component
public class ChatRoomOwnerPolicyRegistry {

    private final Map<String, ChatRoomOwnerPolicy> policies;

    public ChatRoomOwnerPolicyRegistry(List<ChatRoomOwnerPolicy> policies) {
        Map<String, ChatRoomOwnerPolicy> indexed = new HashMap<>();
        for (ChatRoomOwnerPolicy policy : policies) {
            if (policy == null) {
                throw new IllegalStateException("Chat room owner policy namespace는 필수입니다.");
            }
            String namespace = policy.namespace();
            if (namespace == null || namespace.isBlank()) {
                throw new IllegalStateException("Chat room owner policy namespace는 필수입니다.");
            }
            if (indexed.putIfAbsent(namespace, policy) != null) {
                throw new IllegalStateException(
                    "Chat room owner policy namespace가 중복되었습니다: " + namespace);
            }
        }
        this.policies = Map.copyOf(indexed);
    }

    public ChatRoomOwnerPolicy requirePolicy(String namespace) {
        ChatRoomOwnerPolicy policy = policies.get(namespace);
        if (policy == null) {
            throw new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        return policy;
    }

    public List<String> namespaces() {
        return policies.keySet().stream().sorted().toList();
    }
}
