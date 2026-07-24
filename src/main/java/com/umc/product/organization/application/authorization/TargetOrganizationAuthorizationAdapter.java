package com.umc.product.organization.application.authorization;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.EvaluateRegisteredPolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.AuthorizationRoleTuple;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.OrganizationType;

@Component
public class TargetOrganizationAuthorizationAdapter
    implements PolicyRolloutEvaluator<OrganizationAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetOrganizationAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(OrganizationAuthorizationContext context) {
        var subject = context.subject();
        boolean superAdmin = subject.map(value -> value.isSuperAdmin()).orElse(false);
        boolean activeCentralCore = superAdmin || subject.stream()
            .flatMap(value -> value.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        Long schoolId = context.legacySubject().schoolId();
        boolean activeSchoolAdmin = superAdmin || activeSchoolRole(
            context,
            schoolId,
            false);
        boolean activeSchoolCore = superAdmin || activeSchoolRole(
            context,
            schoolId,
            true);

        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            OrganizationPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(
                    OrganizationPolicyAttributes.ACTIVE_CENTRAL_CORE,
                    new PolicyValue.BooleanValue(activeCentralCore))
                .put(
                    OrganizationPolicyAttributes.ACTIVE_SCHOOL_ADMIN,
                    new PolicyValue.BooleanValue(activeSchoolAdmin))
                .put(
                    OrganizationPolicyAttributes.ACTIVE_SCHOOL_CORE,
                    new PolicyValue.BooleanValue(activeSchoolCore))
                .build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }

    private boolean activeSchoolRole(
        OrganizationAuthorizationContext context,
        Long schoolId,
        boolean coreOnly
    ) {
        if (schoolId == null) {
            return false;
        }
        return context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> Objects.equals(role.organizationId(), schoolId))
            .map(AuthorizationRoleTuple::roleType)
            .anyMatch(role -> coreOnly
                ? role.isAtLeastSchoolCore()
                : role.isAtLeastSchoolAdmin());
    }
}
