package com.umc.product.maintenance.application.authorization;

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
public class TargetMaintenanceAuthorizationAdapter
    implements PolicyRolloutEvaluator<MaintenanceAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetMaintenanceAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(MaintenanceAuthorizationContext context) {
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            MaintenancePolicyDomainSchema.BUNDLE_KEY,
            MaintenancePolicyAction.BYPASS.id(),
            PolicyAttributeSet.builder()
                .put(
                    MaintenancePolicyDomainSchema.SUPER_ADMIN,
                    new PolicyValue.BooleanValue(context.subject().isSuperAdmin()))
                .build(),
            context.subject().evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
