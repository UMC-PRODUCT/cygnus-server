package com.umc.product.analytics.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class AnalyticsExpectedDifference
    implements PolicyExpectedDifference<AnalyticsAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        AnalyticsAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        return legacyDecision
            && !targetDecision
            && context.subject().stream()
                .flatMap(subject -> subject.roles().stream())
                .anyMatch(role -> !role.isActiveAt(context.evaluatedAt()));
    }
}
