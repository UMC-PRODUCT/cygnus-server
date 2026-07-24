package com.umc.product.feedback.application.authorization;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

@Component
public class LegacyFeedbackAuthorizationAdapter
    implements PolicyRolloutEvaluator<FeedbackAuthorizationContext, FeedbackAuthorizationDecision> {

    @Override
    public PolicyRolloutEvaluation<FeedbackAuthorizationDecision> evaluate(
        FeedbackAuthorizationContext context
    ) {
        if (context.action() == FeedbackPolicyAction.RESPONSE_SUBMIT) {
            return success(FeedbackAuthorizationDecision.allowWithoutOutcome());
        }
        Optional<UserFeedbackTargetType> targetType = resolveTargetType(context);
        return success(targetType
            .map(FeedbackAuthorizationDecision::allow)
            .orElseGet(FeedbackAuthorizationDecision::deny));
    }

    private Optional<UserFeedbackTargetType> resolveTargetType(
        FeedbackAuthorizationContext context
    ) {
        if (context.targetGisuId() == null) {
            return Optional.empty();
        }
        if (context.legacyCentralMember()) {
            return Optional.of(UserFeedbackTargetType.ADMIN);
        }
        boolean activeChallenger = context.legacyChallengerHistory().stream()
            .anyMatch(challenger -> challenger.gisuId().equals(context.targetGisuId()));
        if (!activeChallenger) {
            return Optional.empty();
        }
        boolean previousGisu = context.legacyChallengerHistory().stream()
            .anyMatch(challenger -> !challenger.gisuId().equals(context.targetGisuId()));
        if (previousGisu) {
            return Optional.of(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
        }
        boolean generationTenPlan = Long.valueOf(10).equals(context.targetGeneration())
            && context.legacyChallengerHistory().stream()
                .filter(challenger -> challenger.gisuId().equals(context.targetGisuId()))
                .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN);
        return Optional.of(generationTenPlan
            ? UserFeedbackTargetType.EXPERIENCED_CHALLENGER
            : UserFeedbackTargetType.NEW_CHALLENGER);
    }

    private PolicyRolloutEvaluation<FeedbackAuthorizationDecision> success(
        FeedbackAuthorizationDecision decision
    ) {
        return new PolicyRolloutEvaluation.Success<>(decision);
    }
}
