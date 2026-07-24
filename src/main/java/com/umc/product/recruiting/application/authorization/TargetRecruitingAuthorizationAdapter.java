package com.umc.product.recruiting.application.authorization;

import java.util.Objects;

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
import com.umc.product.common.domain.enums.OrganizationType;

@Component
public class TargetRecruitingAuthorizationAdapter
    implements PolicyRolloutEvaluator<RecruitingAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetRecruitingAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(RecruitingAuthorizationContext context) {
        boolean superAdmin = context.subject().map(subject -> subject.isSuperAdmin()).orElse(false);
        var activeRoles = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean anyCentralCore = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean anySchoolCore = activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastSchoolCore());
        boolean centralInTarget = context.targetGisuId() != null
            && activeRoles.stream()
                .filter(role -> role.gisuId() == context.targetGisuId())
                .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean schoolForTarget = context.targetGisuId() != null
            && context.targetSchoolId() != null
            && activeRoles.stream()
                .filter(role -> role.gisuId() == context.targetGisuId())
                .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
                .filter(role -> Objects.equals(role.organizationId(), context.targetSchoolId()))
                .anyMatch(role -> role.roleType().isAtLeastSchoolCore());
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            RecruitingPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(RecruitingPolicyAttributes.RESOURCE_SPECIFIED, bool(context.resourceSpecified()))
                .put(RecruitingPolicyAttributes.SUPER_ADMIN, bool(superAdmin))
                .put(RecruitingPolicyAttributes.ACTIVE_ANY_CENTRAL_CORE, bool(anyCentralCore))
                .put(RecruitingPolicyAttributes.ACTIVE_ANY_SCHOOL_CORE, bool(anySchoolCore))
                .put(RecruitingPolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET, bool(centralInTarget))
                .put(RecruitingPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET, bool(schoolForTarget))
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
