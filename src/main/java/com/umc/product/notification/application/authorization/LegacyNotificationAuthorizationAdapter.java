package com.umc.product.notification.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyNotificationAuthorizationAdapter
    implements PolicyRolloutEvaluator<NotificationAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(NotificationAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case SEND_FCM -> context.legacySubject().orElseThrow().roleAttributes().stream()
                .anyMatch(role -> role.roleType().isAtLeastCentralCore());
            case DELETE_TOKEN -> context.tokenOwner()
                || context.legacySubject().stream()
                    .flatMap(subject -> subject.roleAttributes().stream())
                    .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
