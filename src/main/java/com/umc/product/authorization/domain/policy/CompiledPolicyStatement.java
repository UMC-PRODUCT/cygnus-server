package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.Objects;

public record CompiledPolicyStatement(
        String id,
        List<String> actions,
        PolicyEffect effect,
        PolicyCondition condition,
        List<CompiledPolicyOutcome> outcomes) {

    public CompiledPolicyStatement {
        Objects.requireNonNull(id);
        actions = List.copyOf(actions);
        Objects.requireNonNull(effect);
        Objects.requireNonNull(condition);
        outcomes = List.copyOf(outcomes);
    }
}
