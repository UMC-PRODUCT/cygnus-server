package com.umc.product.authorization.domain.policy;

import java.util.Objects;
import java.util.Set;

public record ActionSchema(
        String actionId,
        Set<String> requiredAttributes,
        Set<String> optionalAttributes,
        Set<String> allowedOutcomes) {

    public ActionSchema {
        Objects.requireNonNull(actionId);
        requiredAttributes = Set.copyOf(requiredAttributes);
        optionalAttributes = Set.copyOf(optionalAttributes);
        allowedOutcomes = Set.copyOf(allowedOutcomes);
        if (!java.util.Collections.disjoint(requiredAttributes, optionalAttributes)) {
            throw new IllegalArgumentException("Required and optional attributes must be disjoint");
        }
    }

    public Set<String> allowedAttributes() {
        Set<String> result = new java.util.LinkedHashSet<>(requiredAttributes);
        result.addAll(optionalAttributes);
        return Set.copyOf(result);
    }
}
