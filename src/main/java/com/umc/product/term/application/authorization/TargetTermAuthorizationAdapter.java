package com.umc.product.term.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.EvaluateRegisteredPolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class TargetTermAuthorizationAdapter
    implements PolicyRolloutEvaluator<TermAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetTermAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(TermAuthorizationContext context) {
        var subject = context.subject()
            .orElseThrow(() -> new IllegalStateException("Term target policy subject snapshot이 없습니다."));
        PolicyAttributeSet attributes = PolicyAttributeSet.builder()
            .put(
                TermPolicyAttributes.SUPER_ADMIN,
                new PolicyValue.BooleanValue(subject.isSuperAdmin()))
            .build();
        PolicyEvaluationResult result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            TermPolicyDomainSchema.BUNDLE_KEY,
            TermPolicyAction.CREATE.id(),
            attributes,
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        PolicyDecision decision = (PolicyDecision) result;
        return new PolicyRolloutEvaluation.Success<>(decision.effect() == PolicyEffect.ALLOW);
    }
}
