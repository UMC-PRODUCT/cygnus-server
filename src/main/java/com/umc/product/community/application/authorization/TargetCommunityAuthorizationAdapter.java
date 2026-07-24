package com.umc.product.community.application.authorization;

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
public class TargetCommunityAuthorizationAdapter
    implements PolicyRolloutEvaluator<CommunityAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetCommunityAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CommunityAuthorizationContext context) {
        boolean superAdmin = context.subject()
            .map(subject -> subject.isSuperAdmin())
            .orElse(false);
        boolean activeCentralCore = context.subject().stream()
            .flatMap(subject -> subject.roles().stream())
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
        boolean isAuthor = context.authorMemberId() != null
            && context.authorMemberId().equals(context.legacySubject().memberId());
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            CommunityPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(CommunityPolicyAttributes.AUTHENTICATED,
                    bool(context.legacySubject().memberId() != null))
                .put(CommunityPolicyAttributes.AUTHOR_HAS_CHALLENGER_HISTORY,
                    bool(context.authorHasChallengerHistory()))
                .put(CommunityPolicyAttributes.IS_AUTHOR, bool(isAuthor))
                .put(CommunityPolicyAttributes.SUPER_ADMIN, bool(superAdmin))
                .put(CommunityPolicyAttributes.ACTIVE_CENTRAL_CORE, bool(activeCentralCore))
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
