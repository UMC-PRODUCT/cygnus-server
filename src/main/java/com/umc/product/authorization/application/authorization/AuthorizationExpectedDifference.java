package com.umc.product.authorization.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class AuthorizationExpectedDifference
    implements PolicyExpectedDifference<AuthorizationPolicyContext, Boolean> {

    @Override
    public boolean matches(
        AuthorizationPolicyContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        if (context.action() == AuthorizationPolicyAction.CHALLENGER_ROLE_READ
            || !legacyDecision
            || targetDecision
            || context.subject().isEmpty()) {
            return false;
        }
        var subject = context.subject().orElseThrow();
        if (subject.isSuperAdmin()) {
            return false;
        }
        boolean hasLegacyRole = context.legacySubject().roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean hasActiveRole = subject.roles().stream()
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        return hasLegacyRole && !hasActiveRole;
    }
}
