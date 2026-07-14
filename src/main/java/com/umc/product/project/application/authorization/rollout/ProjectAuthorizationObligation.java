package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

import com.umc.product.authorization.domain.policy.PolicyValue;

public record ProjectAuthorizationObligation(String key, PolicyValue value) {
    public ProjectAuthorizationObligation {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
    }
}
