package com.umc.product.authorization.domain.policy.rollout;

import java.util.Objects;
import java.util.Optional;

public record PolicyRolloutCoordinationResult<D>(
    PolicyRolloutMode mode,
    PolicyRolloutEvaluation<D> legacy,
    Optional<PolicyRolloutEvaluation<D>> target,
    Optional<PolicyRolloutClassification> classification,
    PolicyRolloutEvaluation<D> authoritative
) {

    public PolicyRolloutCoordinationResult {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(legacy);
        target = Objects.requireNonNull(target);
        classification = Objects.requireNonNull(classification);
        Objects.requireNonNull(authoritative);
        if (mode == PolicyRolloutMode.LEGACY && (target.isPresent() || classification.isPresent())) {
            throw new IllegalArgumentException("LEGACY mode는 target 평가 결과를 가질 수 없습니다.");
        }
        if (mode != PolicyRolloutMode.LEGACY && (target.isEmpty() || classification.isEmpty())) {
            throw new IllegalArgumentException("SHADOW/ENFORCE mode는 target 평가와 분류가 필요합니다.");
        }
    }
}
