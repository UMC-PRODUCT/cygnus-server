package com.umc.product.term.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyTermAuthorizationAdapter
    implements PolicyRolloutEvaluator<TermAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(TermAuthorizationContext context) {
        boolean allowed = context.legacySubject().toAuthoritySnapshot().isSuperAdmin();
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
