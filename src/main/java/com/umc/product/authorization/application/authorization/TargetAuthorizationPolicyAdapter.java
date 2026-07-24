package com.umc.product.authorization.application.authorization;

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
public class TargetAuthorizationPolicyAdapter
    implements PolicyRolloutEvaluator<AuthorizationPolicyContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetAuthorizationPolicyAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AuthorizationPolicyContext context) {
        boolean activeCentralCore = context.subject()
            .map(subject -> subject.isSuperAdmin()
                || subject.roles().stream()
                    .filter(role -> role.isActiveAt(context.evaluatedAt()))
                    .anyMatch(role -> role.roleType().isAtLeastCentralCore()))
            .orElse(false);
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            AuthorizationPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(
                    AuthorizationPolicyAttributes.AUTHENTICATED,
                    new PolicyValue.BooleanValue(context.legacySubject().memberId() != null))
                .put(
                    AuthorizationPolicyAttributes.ACTIVE_CENTRAL_CORE,
                    new PolicyValue.BooleanValue(activeCentralCore))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
