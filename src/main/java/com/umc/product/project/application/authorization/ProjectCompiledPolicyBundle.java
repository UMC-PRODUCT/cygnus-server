package com.umc.product.project.application.authorization;

import java.util.Objects;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

public record ProjectCompiledPolicyBundle(CompiledPolicyBundle value) {
    public ProjectCompiledPolicyBundle {
        Objects.requireNonNull(value);
    }
}
