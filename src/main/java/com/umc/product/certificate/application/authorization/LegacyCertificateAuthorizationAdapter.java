package com.umc.product.certificate.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyCertificateAuthorizationAdapter
    implements PolicyRolloutEvaluator<CertificateAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CertificateAuthorizationContext context) {
        return new PolicyRolloutEvaluation.Success<>(
            context.subject().isSuperAdmin()
                || context.legacyCentralCoreInTargetGisu());
    }
}
