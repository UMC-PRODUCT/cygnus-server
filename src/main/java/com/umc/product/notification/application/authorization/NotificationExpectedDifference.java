package com.umc.product.notification.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class NotificationExpectedDifference
    implements PolicyExpectedDifference<NotificationAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        NotificationAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        if ((context.action() != NotificationPolicyAction.SEND_FCM
            && context.action() != NotificationPolicyAction.DELETE_TOKEN)
            || !legacyDecision
            || targetDecision
            || context.subject().isEmpty()) {
            return false;
        }
        boolean hasLegacyCentralCore = context.legacySubject().orElseThrow().roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean hasActiveCentralCore = context.subject().orElseThrow().roles().stream()
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        return hasLegacyCentralCore && !hasActiveCentralCore;
    }
}
