package com.umc.product.maintenance.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyMaintenanceAuthorizationAdapter
    implements PolicyRolloutEvaluator<MaintenanceAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(MaintenanceAuthorizationContext context) {
        return new PolicyRolloutEvaluation.Success<>(context.subject().isSuperAdmin());
    }
}
