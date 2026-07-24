package com.umc.product.curriculum.application.authorization;

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
public class TargetCurriculumAuthorizationAdapter
    implements PolicyRolloutEvaluator<CurriculumAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetCurriculumAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CurriculumAuthorizationContext context) {
        boolean superAdmin = context.subject().map(subject -> subject.isSuperAdmin()).orElse(false);
        var activeRoles = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean activeCentral = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        boolean activeSchool = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastSchoolAdmin());
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            CurriculumPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(CurriculumPolicyAttributes.SUPER_ADMIN, bool(superAdmin))
                .put(CurriculumPolicyAttributes.ACTIVE_CENTRAL_MEMBER, bool(activeCentral))
                .put(CurriculumPolicyAttributes.ACTIVE_SCHOOL_ADMIN, bool(activeSchool))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }

    private PolicyValue.BooleanValue bool(boolean value) {
        return new PolicyValue.BooleanValue(value);
    }
}
