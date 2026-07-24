package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record PolicySurfaceDescriptor(
    String namespace,
    String id,
    String handler,
    PolicySurfaceType type,
    String actionId,
    String moduleId,
    PolicySurfaceGate gate
) implements Comparable<PolicySurfaceDescriptor> {

    public PolicySurfaceDescriptor {
        requireText(namespace, "namespace");
        requireText(id, "id");
        requireText(handler, "handler");
        Objects.requireNonNull(type);
        requireText(actionId, "actionId");
        requireText(moduleId, "moduleId");
        Objects.requireNonNull(gate);
    }

    @Override
    public int compareTo(PolicySurfaceDescriptor other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : id.compareTo(other.id);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Policy surface " + field + "는 비어 있을 수 없습니다.");
        }
    }
}
