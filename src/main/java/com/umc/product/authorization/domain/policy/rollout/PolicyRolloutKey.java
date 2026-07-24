package com.umc.product.authorization.domain.policy.rollout;

import java.util.Objects;

public record PolicyRolloutKey(String namespace, String actionId)
    implements Comparable<PolicyRolloutKey> {

    public PolicyRolloutKey {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(actionId);
        if (namespace.isBlank() || actionId.isBlank()) {
            throw new IllegalArgumentException("Policy rollout namespace와 action은 비어 있을 수 없습니다.");
        }
    }

    @Override
    public int compareTo(PolicyRolloutKey other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : actionId.compareTo(other.actionId);
    }
}
