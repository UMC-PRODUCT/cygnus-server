package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public record ProjectExpectedDifferenceRule(
    ProjectExpectedDifferenceId id,
    ProjectExpectedDifferenceContextPredicate contextPredicate,
    ProjectAuthorizationDecision expectedLegacy,
    ProjectAuthorizationDecision expectedTarget
) {
    public ProjectExpectedDifferenceRule {
        Objects.requireNonNull(id);
        Objects.requireNonNull(contextPredicate);
        Objects.requireNonNull(expectedLegacy);
        Objects.requireNonNull(expectedTarget);
    }

    public boolean matches(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    ) {
        return contextPredicate.matches(request)
            && expectedLegacy.equals(legacy)
            && expectedTarget.equals(target);
    }
}
