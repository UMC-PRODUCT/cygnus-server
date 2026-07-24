package com.umc.product.feedback.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

final class FeedbackExpectedDifference
    implements PolicyExpectedDifference<
        FeedbackAuthorizationContext,
        FeedbackAuthorizationDecision> {

    @Override
    public boolean matches(
        FeedbackAuthorizationContext context,
        FeedbackAuthorizationDecision legacyDecision,
        FeedbackAuthorizationDecision targetDecision
    ) {
        return legacyDecision.allowed()
            && !legacyDecision.equals(targetDecision)
            && context.subject().roles().stream()
                .filter(role -> context.targetGisuId() != null
                    && role.gisuId() == context.targetGisuId())
                .anyMatch(role -> role.roleType().isAtLeastCentralMember()
                    && !role.isActiveAt(context.evaluatedAt()));
    }
}
