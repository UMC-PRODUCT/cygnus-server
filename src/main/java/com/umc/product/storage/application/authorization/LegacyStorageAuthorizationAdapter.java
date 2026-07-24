package com.umc.product.storage.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyStorageAuthorizationAdapter
    implements PolicyRolloutEvaluator<StorageAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(StorageAuthorizationContext context) {
        return new PolicyRolloutEvaluation.Success<>(
            context.uploader() || context.superAdmin());
    }
}
