package com.umc.product.member.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class MemberExpectedDifference
    implements PolicyExpectedDifference<MemberAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        MemberAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        if (context.action() != MemberPolicyAction.DELETE
            || !legacyDecision
            || targetDecision
            || context.subject().isEmpty()
            || context.subject().orElseThrow().isSuperAdmin()) {
            return false;
        }
        return context.legacySubject().roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore())
            && context.subject().orElseThrow().roles().stream()
                .filter(role -> role.isActiveAt(context.evaluatedAt()))
                .noneMatch(role -> role.roleType().isAtLeastCentralCore());
    }
}
