package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record OutcomeSchema(
        String outcomeKey, PolicyValueType type, OutcomeMergeStrategy mergeStrategy, List<String> dominanceOrder) {

    public OutcomeSchema {
        Objects.requireNonNull(outcomeKey);
        Objects.requireNonNull(type);
        Objects.requireNonNull(mergeStrategy);
        dominanceOrder = List.copyOf(dominanceOrder);
        if (Set.copyOf(dominanceOrder).size() != dominanceOrder.size()) {
            throw new IllegalArgumentException("Dominance order must not contain duplicates");
        }
        if (mergeStrategy == OutcomeMergeStrategy.DOMINANCE && dominanceOrder.isEmpty()) {
            throw new IllegalArgumentException("Dominance outcome requires an order");
        }
        if (mergeStrategy != OutcomeMergeStrategy.DOMINANCE && !dominanceOrder.isEmpty()) {
            throw new IllegalArgumentException("Only dominance outcomes may define an order");
        }
        if (mergeStrategy == OutcomeMergeStrategy.BOOLEAN_OR && type != PolicyValueType.BOOLEAN) {
            throw new IllegalArgumentException("Boolean-or outcome requires a boolean type");
        }
        if (mergeStrategy == OutcomeMergeStrategy.SET_UNION && !type.isSet()) {
            throw new IllegalArgumentException("Set-union outcome requires a set type");
        }
        if (mergeStrategy == OutcomeMergeStrategy.DOMINANCE
                && type != PolicyValueType.ENUM
                && type != PolicyValueType.STRING) {
            throw new IllegalArgumentException("Dominance outcome requires an enum or string type");
        }
        if (type == PolicyValueType.ENUM && dominanceOrder.isEmpty()) {
            throw new IllegalArgumentException("Enum outcome requires a symbol catalog");
        }
    }
}
