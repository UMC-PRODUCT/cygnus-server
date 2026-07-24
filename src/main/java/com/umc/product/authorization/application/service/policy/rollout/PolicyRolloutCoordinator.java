package com.umc.product.authorization.application.service.policy.rollout;

import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutClassification;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutCoordinationResult;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

public final class PolicyRolloutCoordinator<C, D> {

    private static final String UNCAUGHT_FAILURE_CODE = "POLICY_EVALUATION_FAILED";

    private final PolicyRolloutEvaluator<C, D> legacyEvaluator;
    private final PolicyRolloutEvaluator<C, D> targetEvaluator;
    private final PolicyExpectedDifference<C, D> expectedDifference;
    private final PolicyRolloutModeResolver modeResolver;

    public PolicyRolloutCoordinator(
        PolicyRolloutEvaluator<C, D> legacyEvaluator,
        PolicyRolloutEvaluator<C, D> targetEvaluator,
        PolicyExpectedDifference<C, D> expectedDifference,
        PolicyRolloutModeResolver modeResolver
    ) {
        this.legacyEvaluator = Objects.requireNonNull(legacyEvaluator);
        this.targetEvaluator = Objects.requireNonNull(targetEvaluator);
        this.expectedDifference = Objects.requireNonNull(expectedDifference);
        this.modeResolver = Objects.requireNonNull(modeResolver);
    }

    public PolicyRolloutCoordinationResult<D> coordinate(PolicyRolloutKey key, C context) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(context);
        PolicyRolloutMode mode = modeResolver.resolve(key);
        PolicyRolloutEvaluation<D> legacy = safelyEvaluate(legacyEvaluator, context);
        if (mode == PolicyRolloutMode.LEGACY) {
            return new PolicyRolloutCoordinationResult<>(
                mode,
                legacy,
                Optional.empty(),
                Optional.empty(),
                legacy);
        }

        PolicyRolloutEvaluation<D> target = safelyEvaluate(targetEvaluator, context);
        PolicyRolloutClassification classification = classify(context, legacy, target);
        PolicyRolloutEvaluation<D> authoritative =
            mode == PolicyRolloutMode.SHADOW ? legacy : target;
        return new PolicyRolloutCoordinationResult<>(
            mode,
            legacy,
            Optional.of(target),
            Optional.of(classification),
            authoritative);
    }

    private PolicyRolloutEvaluation<D> safelyEvaluate(
        PolicyRolloutEvaluator<C, D> evaluator,
        C context
    ) {
        try {
            return Objects.requireNonNull(evaluator.evaluate(context));
        } catch (RuntimeException exception) {
            return new PolicyRolloutEvaluation.Failure<>(UNCAUGHT_FAILURE_CODE);
        }
    }

    private PolicyRolloutClassification classify(
        C context,
        PolicyRolloutEvaluation<D> legacy,
        PolicyRolloutEvaluation<D> target
    ) {
        if (target instanceof PolicyRolloutEvaluation.Failure<D>) {
            return PolicyRolloutClassification.TARGET_FAILURE;
        }
        if (legacy instanceof PolicyRolloutEvaluation.Failure<D>) {
            return PolicyRolloutClassification.LEGACY_FAILURE;
        }
        D legacyDecision = ((PolicyRolloutEvaluation.Success<D>) legacy).decision();
        D targetDecision = ((PolicyRolloutEvaluation.Success<D>) target).decision();
        if (legacyDecision.equals(targetDecision)) {
            return PolicyRolloutClassification.MATCH;
        }
        return expectedDifference.matches(context, legacyDecision, targetDecision)
            ? PolicyRolloutClassification.EXPECTED_DIFFERENCE
            : PolicyRolloutClassification.UNEXPECTED_DIFFERENCE;
    }
}
