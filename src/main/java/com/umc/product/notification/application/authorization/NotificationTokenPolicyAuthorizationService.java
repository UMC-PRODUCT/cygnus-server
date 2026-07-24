package com.umc.product.notification.application.authorization;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;

@Service
public class NotificationTokenPolicyAuthorizationService {

    private final BooleanPolicyRolloutExecutor<NotificationAuthorizationContext> rollout;
    private final Clock clock;

    public NotificationTokenPolicyAuthorizationService(
        LegacyNotificationAuthorizationAdapter legacyEvaluator,
        TargetNotificationAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.rollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            PolicyExpectedDifference.none(),
            modeResolver,
            observer,
            registry,
            NotificationPolicyDomainSchema.BUNDLE_KEY,
            NotificationPolicyAction.DELETE_TOKEN.id());
        this.clock = clock;
    }

    public boolean canDelete(boolean tokenOwner) {
        var evaluatedAt = clock.instant();
        return rollout.evaluate(
            new NotificationAuthorizationContext(
                NotificationPolicyAction.DELETE_TOKEN,
                Optional.empty(),
                Optional.empty(),
                tokenOwner,
                evaluatedAt),
            evaluatedAt);
    }
}
