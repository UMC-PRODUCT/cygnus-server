package com.umc.product.curriculum.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class CurriculumExpectedDifference
    implements PolicyExpectedDifference<CurriculumAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        CurriculumAuthorizationContext context,
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
