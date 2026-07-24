package com.umc.product.notification.application.authorization;

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
public class TargetNotificationAuthorizationAdapter
    implements PolicyRolloutEvaluator<NotificationAuthorizationContext, Boolean> {

    private final EvaluateRegisteredPolicyUseCase evaluator;

    public TargetNotificationAuthorizationAdapter(EvaluateRegisteredPolicyUseCase evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(NotificationAuthorizationContext context) {
        PolicyAttributeSet attributes = switch (context.action()) {
            case SEND_FCM -> PolicyAttributeSet.builder()
                .put(
                    NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE,
                    new PolicyValue.BooleanValue(activeCentralCore(context)))
                .build();
            case DELETE_TOKEN -> deleteTokenAttributes(context);
        };
        var result = evaluator.evaluate(new RegisteredPolicyEvaluationRequest(
            NotificationPolicyDomainSchema.BUNDLE_KEY,
            context.action().id(),
            attributes,
            context.evaluatedAt()));
        if (result instanceof PolicyEvaluationFailure failure) {
            return new PolicyRolloutEvaluation.Failure<>(failure.failureCode().name());
        }
        return new PolicyRolloutEvaluation.Success<>(
            ((PolicyDecision) result).effect() == PolicyEffect.ALLOW);
    }

    private boolean activeCentralCore(NotificationAuthorizationContext context) {
        var subject = context.subject()
            .orElseThrow(() -> new IllegalStateException(
                "Notification target policy subject snapshot이 없습니다."));
        return subject.roles().stream()
            .filter(role -> role.isActiveAt(context.evaluatedAt()))
            .anyMatch(role -> role.roleType().isAtLeastCentralCore());
    }

    private PolicyAttributeSet deleteTokenAttributes(NotificationAuthorizationContext context) {
        PolicyAttributeSet.Builder builder = PolicyAttributeSet.builder()
            .put(
                NotificationPolicyAttributes.TOKEN_OWNER,
                new PolicyValue.BooleanValue(context.tokenOwner()));
        context.subject().ifPresent(ignored -> builder.put(
            NotificationPolicyAttributes.ACTIVE_CENTRAL_CORE,
            new PolicyValue.BooleanValue(activeCentralCore(context))));
        return builder.build();
    }
}
