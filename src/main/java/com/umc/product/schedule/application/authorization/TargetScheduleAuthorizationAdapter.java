package com.umc.product.schedule.application.authorization;

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
public class TargetScheduleAuthorizationAdapter
    implements PolicyRolloutEvaluator<ScheduleAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetScheduleAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(ScheduleAuthorizationContext context) {
        boolean superAdmin = context.subject()
            .map(subject -> subject.isSuperAdmin())
            .orElse(false);
        var activeRoles = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean activeOperatingStaff = activeRoles.stream()
            .anyMatch(role -> isOperatingRole(role.roleType()));
        boolean activeTargetGisuStaff = context.targetGisuId() != null
            && activeRoles.stream()
                .filter(role -> role.gisuId() == context.targetGisuId())
                .anyMatch(role -> isOperatingRole(role.roleType()));
        boolean challengerHistory = context.subject()
            .map(subject -> !subject.challengers().isEmpty())
            .orElse(!context.legacySubject().gisuChallengerInfos().isEmpty());

        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            SchedulePolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(SchedulePolicyAttributes.SUPER_ADMIN, bool(superAdmin))
                .put(SchedulePolicyAttributes.CHALLENGER_HISTORY, bool(challengerHistory))
                .put(SchedulePolicyAttributes.AUTHOR, bool(context.author()))
                .put(SchedulePolicyAttributes.PARTICIPANT, bool(context.participant()))
                .put(SchedulePolicyAttributes.RESOURCE_SPECIFIED, bool(context.resourceSpecified()))
                .put(SchedulePolicyAttributes.ACTIVE_OPERATING_STAFF, bool(activeOperatingStaff))
                .put(SchedulePolicyAttributes.ACTIVE_TARGET_GISU_STAFF, bool(activeTargetGisuStaff))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }

    private boolean isOperatingRole(com.umc.product.common.domain.enums.ChallengerRoleType roleType) {
        return roleType.isAtLeastCentralMember() || roleType.isAtLeastSchoolAdmin();
    }

    private PolicyValue.BooleanValue bool(boolean value) {
        return new PolicyValue.BooleanValue(value);
    }
}
