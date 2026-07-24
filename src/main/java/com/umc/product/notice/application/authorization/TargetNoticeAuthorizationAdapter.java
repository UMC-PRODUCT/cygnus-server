package com.umc.product.notice.application.authorization;

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
public class TargetNoticeAuthorizationAdapter
    implements PolicyRolloutEvaluator<NoticeAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;
    private final NoticePolicyRelationResolver relationResolver;

    public TargetNoticeAuthorizationAdapter(
        EvaluateRegisteredPolicyUseCase evaluator,
        NoticePolicyRelationResolver relationResolver
    ) {
        this.evaluator = evaluator;
        this.relationResolver = relationResolver;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(NoticeAuthorizationContext context) {
        NoticePolicyRelations relation = relationResolver.resolve(context);
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            NoticePolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            PolicyAttributeSet.builder()
                .put(NoticePolicyAttributes.SUPER_ADMIN, bool(relation.superAdmin()))
                .put(NoticePolicyAttributes.ACTIVE_CENTRAL_CORE, bool(relation.activeCentralCore()))
                .put(NoticePolicyAttributes.TARGET_CHALLENGER, bool(relation.targetChallenger()))
                .put(
                    NoticePolicyAttributes.ACTIVE_ROLE_CAN_READ_TARGET,
                    bool(relation.activeRoleCanReadTarget()))
                .put(NoticePolicyAttributes.AUTHOR, bool(relation.author()))
                .put(
                    NoticePolicyAttributes.ACTIVE_MANAGER_FOR_TARGET,
                    bool(relation.activeManagerForTarget()))
                .put(
                    NoticePolicyAttributes.ACTIVE_CREATOR_FOR_TARGET,
                    bool(relation.activeCreatorForTarget()))
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
