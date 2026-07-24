package com.umc.product.authorization.application.service.policy.rollout;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.domain.policy.PolicyBundleIdentity;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;

/**
 * Boolean이 아닌 scope, masking, enum outcome에도 같은 rollout 계약을 적용한다.
 */
public final class PolicyDecisionRolloutExecutor<C, D> {

    private static final Logger log = LoggerFactory.getLogger(PolicyDecisionRolloutExecutor.class);

    private final PolicyRolloutCoordinator<C, D> coordinator;
    private final PolicyRolloutObserver observer;
    private final PolicyBundleIdentity targetIdentity;
    private final PolicyRolloutKey rolloutKey;
    private final Function<D, PolicyRolloutObserver.EvaluationEffect> effectClassifier;
    private final D failureDecision;

    public PolicyDecisionRolloutExecutor(
        PolicyRolloutEvaluator<C, D> legacyEvaluator,
        PolicyRolloutEvaluator<C, D> targetEvaluator,
        PolicyExpectedDifference<C, D> expectedDifference,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        PolicyBundleKey bundleKey,
        String actionId,
        Function<D, PolicyRolloutObserver.EvaluationEffect> effectClassifier,
        D failureDecision
    ) {
        this.coordinator = new PolicyRolloutCoordinator<>(
            legacyEvaluator,
            targetEvaluator,
            expectedDifference,
            modeResolver);
        this.observer = Objects.requireNonNull(observer);
        this.targetIdentity = PolicyBundleIdentity.from(registry.require(bundleKey));
        this.rolloutKey = new PolicyRolloutKey(bundleKey.namespace(), actionId);
        this.effectClassifier = Objects.requireNonNull(effectClassifier);
        this.failureDecision = Objects.requireNonNull(failureDecision);
    }

    public D evaluate(C context, Instant evaluatedAt) {
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
        if (result.authoritative() instanceof PolicyRolloutEvaluation.Success<D> success) {
            return success.decision();
        }
        return failureDecision;
    }

    private PolicyRolloutObserver.EvaluationEffect effect(
        PolicyRolloutEvaluation<D> evaluation
    ) {
        if (evaluation instanceof PolicyRolloutEvaluation.Failure<D>) {
            return PolicyRolloutObserver.EvaluationEffect.FAILURE;
        }
        D decision = ((PolicyRolloutEvaluation.Success<D>) evaluation).decision();
        return effectClassifier.apply(decision);
    }
}
