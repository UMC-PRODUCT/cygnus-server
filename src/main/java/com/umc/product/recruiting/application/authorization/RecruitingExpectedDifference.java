package com.umc.product.recruiting.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class RecruitingExpectedDifference
    implements PolicyExpectedDifference<RecruitingAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        RecruitingAuthorizationContext context,
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
