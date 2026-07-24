package com.umc.product.certificate.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.EvaluateRegisteredPolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class TargetCertificateAuthorizationAdapter
    implements PolicyRolloutEvaluator<CertificateAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetCertificateAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CertificateAuthorizationContext context) {
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            CertificatePolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(
                    CertificatePolicyAttributes.SUPER_ADMIN,
                    new PolicyValue.BooleanValue(context.subject().isSuperAdmin()))
                .put(
                    CertificatePolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET_GISU,
                    new PolicyValue.BooleanValue(context.activeCentralCoreInTargetGisu()))
                .build(),
            context.subject().evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
