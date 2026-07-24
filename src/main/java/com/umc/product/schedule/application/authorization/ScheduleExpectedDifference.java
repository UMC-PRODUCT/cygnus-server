package com.umc.product.schedule.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class ScheduleExpectedDifference
    implements PolicyExpectedDifference<ScheduleAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        ScheduleAuthorizationContext context,
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
