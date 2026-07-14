package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public record CompiledPolicyOutcome(String key, PolicyOperand value) {
    public CompiledPolicyOutcome {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
    }
}
