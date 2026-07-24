package com.umc.product.authorization.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyAuthorizationPolicyAdapter
    implements PolicyRolloutEvaluator<AuthorizationPolicyContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AuthorizationPolicyContext context) {
        boolean allowed = switch (context.action()) {
            case CHALLENGER_ROLE_READ -> true;
            case CHALLENGER_ROLE_CREATE, CHALLENGER_ROLE_DELETE ->
                context.legacySubject().toAuthoritySnapshot().isCentralCoreInAnyGisu();
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
