package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record PolicyAttributeKey<V extends PolicyValue>(String name, PolicyValueType type) {

    public PolicyAttributeKey {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Policy attribute key must not be blank");
        }
    }
}
