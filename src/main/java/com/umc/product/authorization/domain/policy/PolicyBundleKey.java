package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record PolicyBundleKey(String namespace, String contextSchemaVersion) implements Comparable<PolicyBundleKey> {

    public PolicyBundleKey {
        requireText(namespace, "Policy namespace");
        requireText(contextSchemaVersion, "Policy context schema version");
    }

    @Override
    public int compareTo(PolicyBundleKey other) {
        int namespaceOrder = namespace.compareTo(Objects.requireNonNull(other).namespace);
        return namespaceOrder != 0
            ? namespaceOrder
            : contextSchemaVersion.compareTo(other.contextSchemaVersion);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
