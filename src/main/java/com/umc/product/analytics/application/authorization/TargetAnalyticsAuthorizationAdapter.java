package com.umc.product.analytics.application.authorization;

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
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class TargetAnalyticsAuthorizationAdapter
    implements PolicyRolloutEvaluator<AnalyticsAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetAnalyticsAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AnalyticsAuthorizationContext context) {
        boolean superAdmin = context.subject()
            .map(subject -> subject.isSuperAdmin())
            .orElse(false);
        var activeRoles = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean central = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        boolean chapter = activeRoles.stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
        boolean school = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastSchoolAdmin());

        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            AnalyticsPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(
                    AnalyticsPolicyAttributes.ACTIVE_SUPER_ADMIN,
                    new PolicyValue.BooleanValue(superAdmin))
                .put(
                    AnalyticsPolicyAttributes.ACTIVE_CENTRAL_MEMBER,
                    new PolicyValue.BooleanValue(central))
                .put(
                    AnalyticsPolicyAttributes.ACTIVE_CHAPTER_PRESIDENT,
                    new PolicyValue.BooleanValue(chapter))
                .put(
                    AnalyticsPolicyAttributes.ACTIVE_SCHOOL_OPERATOR,
                    new PolicyValue.BooleanValue(school))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
