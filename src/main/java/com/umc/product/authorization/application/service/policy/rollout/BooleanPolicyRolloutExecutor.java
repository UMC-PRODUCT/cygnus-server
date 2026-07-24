package com.umc.product.authorization.application.service.policy.rollout;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.domain.policy.PolicyBundleIdentity;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;

public final class BooleanPolicyRolloutExecutor<C> {

    private static final Logger log = LoggerFactory.getLogger(BooleanPolicyRolloutExecutor.class);

    private final PolicyRolloutCoordinator<C, Boolean> coordinator;
    private final PolicyRolloutObserver observer;
    private final PolicyBundleIdentity targetIdentity;
    private final PolicyRolloutKey rolloutKey;

    public BooleanPolicyRolloutExecutor(
        PolicyRolloutEvaluator<C, Boolean> legacyEvaluator,
        PolicyRolloutEvaluator<C, Boolean> targetEvaluator,
        PolicyExpectedDifference<C, Boolean> expectedDifference,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        PolicyBundleKey bundleKey,
        String actionId
    ) {
        this.coordinator = new PolicyRolloutCoordinator<>(
            legacyEvaluator,
            targetEvaluator,
            expectedDifference,
            modeResolver);
        this.observer = observer;
        this.targetIdentity = PolicyBundleIdentity.from(registry.require(bundleKey));
        this.rolloutKey = new PolicyRolloutKey(bundleKey.namespace(), actionId);
    }

    public boolean evaluate(C context, Instant evaluatedAt) {
        long startedAt = System.nanoTime();
        var result = coordinator.coordinate(rolloutKey, context);
        try {
            observer.observe(new PolicyRolloutObserver.Observation(
                rolloutKey,
                result.mode(),
                result.classification(),
                effect(result.legacy()),
                result.target()
                    .map(this::effect)
                    .orElse(PolicyRolloutObserver.EvaluationEffect.NOT_EVALUATED),
                targetIdentity,
                evaluatedAt,
                System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn(
                "Policy rollout 관측에 실패했습니다: namespace={}, action={}",
                rolloutKey.namespace(),
                rolloutKey.actionId());
        }
        if (result.authoritative() instanceof PolicyRolloutEvaluation.Success<Boolean> success) {
            return success.decision();
        }
        return false;
    }

    private PolicyRolloutObserver.EvaluationEffect effect(
        PolicyRolloutEvaluation<Boolean> evaluation
    ) {
        if (evaluation instanceof PolicyRolloutEvaluation.Failure<Boolean>) {
            return PolicyRolloutObserver.EvaluationEffect.FAILURE;
        }
        boolean allowed = ((PolicyRolloutEvaluation.Success<Boolean>) evaluation).decision();
        return allowed
            ? PolicyRolloutObserver.EvaluationEffect.ALLOW
            : PolicyRolloutObserver.EvaluationEffect.DENY;
    }
}
