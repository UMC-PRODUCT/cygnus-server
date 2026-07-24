package com.umc.product.challenger.application.authorization;

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
public class TargetChallengerAuthorizationAdapter
    implements PolicyRolloutEvaluator<ChallengerAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetChallengerAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(ChallengerAuthorizationContext context) {
        boolean superAdmin = context.subject().map(subject -> subject.isSuperAdmin()).orElse(false);
        var activeRoles = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .toList();
        boolean activeCentralCore = superAdmin || activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean activeSchoolCore = superAdmin || activeRoles.stream()
            .anyMatch(role -> role.roleType().isAtLeastSchoolCore());
        boolean activeCentralMemberInTargetGisu = superAdmin
            || context.targetGisuId() != null && activeRoles.stream()
                .filter(role -> role.gisuId() == context.targetGisuId())
                .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        boolean activeSchoolCoreForTarget = superAdmin
            || context.targetGisuId() != null
            && context.targetSchoolId() != null
            && activeRoles.stream()
                .filter(role -> role.gisuId() == context.targetGisuId())
                .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
                .filter(role -> Objects.equals(role.organizationId(), context.targetSchoolId()))
                .anyMatch(role -> role.roleType().isAtLeastSchoolCore());

        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            ChallengerPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(
                    ChallengerPolicyAttributes.ACTIVE_CENTRAL_CORE,
                    new PolicyValue.BooleanValue(activeCentralCore))
                .put(
                    ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE,
                    new PolicyValue.BooleanValue(activeSchoolCore))
                .put(
                    ChallengerPolicyAttributes.ACTIVE_CENTRAL_MEMBER_IN_TARGET_GISU,
                    new PolicyValue.BooleanValue(activeCentralMemberInTargetGisu))
                .put(
                    ChallengerPolicyAttributes.ACTIVE_SCHOOL_CORE_FOR_TARGET,
                    new PolicyValue.BooleanValue(activeSchoolCoreForTarget))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }
}
