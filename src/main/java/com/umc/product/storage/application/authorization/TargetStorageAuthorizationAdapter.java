package com.umc.product.storage.application.authorization;

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
public class TargetStorageAuthorizationAdapter
    implements PolicyRolloutEvaluator<StorageAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetStorageAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(StorageAuthorizationContext context) {
        PolicyAttributeSet.Builder attributes = PolicyAttributeSet.builder()
            .put(
                StoragePolicyAttributes.UPLOADER,
                new PolicyValue.BooleanValue(context.uploader()));
        context.subject().ifPresent(ignored -> attributes.put(
            StoragePolicyAttributes.SUPER_ADMIN,
            new PolicyValue.BooleanValue(context.superAdmin())));
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            StoragePolicyDomainSchema.BUNDLE_KEY,
            StoragePolicyAction.DELETE_FILE.id(),
            attributes.build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
