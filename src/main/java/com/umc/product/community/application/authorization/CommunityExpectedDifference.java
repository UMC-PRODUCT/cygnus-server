package com.umc.product.community.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class CommunityExpectedDifference
    implements PolicyExpectedDifference<CommunityAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        CommunityAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        return legacyDecision
            && !targetDecision
            && (context.action() == CommunityPolicyAction.POST_DELETE
                || context.action() == CommunityPolicyAction.COMMENT_DELETE)
            && context.subject().stream()
                .flatMap(subject -> subject.roles().stream())
                .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                    && !role.isActiveAt(context.evaluatedAt()));
    }
}
