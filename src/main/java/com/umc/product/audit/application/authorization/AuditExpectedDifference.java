package com.umc.product.audit.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class AuditExpectedDifference
    implements PolicyExpectedDifference<AuditAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        AuditAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        if (!legacyDecision || targetDecision || context.subject().isEmpty()) {
            return false;
        }
        var subject = context.subject().orElseThrow();
        if (subject.isSuperAdmin()) {
            return false;
        }
        boolean hasLegacyCentralRole = context.legacySubject().roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        boolean hasActiveCentralRole = subject.roles().stream()
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        return hasLegacyCentralRole && !hasActiveCentralRole;
    }
}
