package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record PolicyResolvedOutcome(String key, PolicyValue value) {
    public PolicyResolvedOutcome {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
    }
}
