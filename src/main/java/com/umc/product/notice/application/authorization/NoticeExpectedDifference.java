package com.umc.product.notice.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class NoticeExpectedDifference
    implements PolicyExpectedDifference<NoticeAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        NoticeAuthorizationContext context,
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
