package com.umc.product.audit.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyAuditAuthorizationAdapter
    implements PolicyRolloutEvaluator<AuditAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AuditAuthorizationContext context) {
        return new PolicyRolloutEvaluation.Success<>(
            context.legacySubject().toAuthoritySnapshot().isCentralMemberInAnyGisu());
    }
}
