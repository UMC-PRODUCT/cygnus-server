package com.umc.product.audit.application.authorization;

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
public class TargetAuditAuthorizationAdapter
    implements PolicyRolloutEvaluator<AuditAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetAuditAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AuditAuthorizationContext context) {
        var subject = context.subject()
            .orElseThrow(() -> new IllegalStateException("Audit target policy subject snapshot이 없습니다."));
        boolean activeCentralMember = subject.isSuperAdmin()
            || subject.roles().stream()
                .filter(role -> role.isActiveAt(context.evaluatedAt()))
                .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            AuditPolicyDomainSchema.BUNDLE_KEY,
            AuditPolicyAction.LIST.id(),
            PolicyAttributeSet.builder()
                .put(
                    AuditPolicyAttributes.ACTIVE_CENTRAL_MEMBER,
                    new PolicyValue.BooleanValue(activeCentralMember))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
