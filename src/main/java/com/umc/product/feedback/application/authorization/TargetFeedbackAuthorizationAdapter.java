package com.umc.product.feedback.application.authorization;

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
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

@Component
public class TargetFeedbackAuthorizationAdapter
    implements PolicyRolloutEvaluator<FeedbackAuthorizationContext, FeedbackAuthorizationDecision> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetFeedbackAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<FeedbackAuthorizationDecision> evaluate(
        FeedbackAuthorizationContext context
    ) {
        boolean activeCentralMember = context.subject().isSuperAdmin()
            || context.targetGisuId() != null
                && context.subject().roles().stream()
                    .filter(role -> role.isActiveAt(context.evaluatedAt()))
                    .filter(role -> role.gisuId() == context.targetGisuId())
                    .anyMatch(role -> role.roleType().isAtLeastCentralMember());
        boolean activeChallenger = context.targetGisuId() != null
            && context.subject().challengers().stream()
                .filter(challenger -> challenger.isActiveAt(context.evaluatedAt()))
                .anyMatch(challenger -> challenger.gisuId() == context.targetGisuId());
        boolean previousGisu = context.targetGisuId() != null
            && context.subject().challengers().stream()
                .anyMatch(challenger -> challenger.gisuId() != context.targetGisuId());
        boolean generationTenPlan = Long.valueOf(10).equals(context.targetGeneration())
            && context.targetGisuId() != null
            && context.subject().challengers().stream()
                .filter(challenger -> challenger.isActiveAt(context.evaluatedAt()))
                .filter(challenger -> challenger.gisuId() == context.targetGisuId())
                .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN);

        PolicyAttributeSet.Builder attributes = PolicyAttributeSet.builder()
            .put(FeedbackPolicyAttributes.ACTIVE_CENTRAL_MEMBER, bool(activeCentralMember))
            .put(FeedbackPolicyAttributes.ACTIVE_CHALLENGER, bool(activeChallenger))
            .put(FeedbackPolicyAttributes.HAS_PREVIOUS_GISU, bool(previousGisu))
            .put(FeedbackPolicyAttributes.GENERATION_TEN_PLAN, bool(generationTenPlan));
        context.resourceTargetType().ifPresent(targetType ->
            attributes.put(
                FeedbackPolicyAttributes.RESOURCE_TARGET_TYPE,
                new PolicyValue.EnumValue(targetType.name())));
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            FeedbackPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            attributes.build(),
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        PolicyDecision decision = (PolicyDecision) result;
        if (decision.effect() != PolicyEffect.ALLOW) {
            return new PolicyRolloutEvaluation.Success<>(FeedbackAuthorizationDecision.deny());
        }
        if (context.action() == FeedbackPolicyAction.RESPONSE_SUBMIT) {
            return new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.allowWithoutOutcome());
        }
        return decision.outcome(FeedbackPolicyOutcomes.TARGET_TYPE)
            .filter(PolicyValue.EnumValue.class::isInstance)
            .map(PolicyValue.EnumValue.class::cast)
            .map(PolicyValue.EnumValue::value)
            .map(UserFeedbackTargetType::valueOf)
            .map(FeedbackAuthorizationDecision::allow)
            .<PolicyRolloutEvaluation<FeedbackAuthorizationDecision>>map(
                PolicyRolloutEvaluation.Success::new)
            .orElseGet(() -> new PolicyRolloutEvaluation.Failure<>("OUTCOME_MISSING"));
    }

    private PolicyValue.BooleanValue bool(boolean value) {
        return new PolicyValue.BooleanValue(value);
    }
}
