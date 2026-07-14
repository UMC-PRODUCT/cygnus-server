package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;
import java.util.Optional;

public record ProjectAuthorizationCoordinationResult(
    ProjectAuthorizationRolloutMode mode,
    ProjectAuthorizationEvaluationResult legacy,
    Optional<ProjectAuthorizationEvaluationResult> target,
    Optional<ProjectAuthorizationClassification> classification,
    ProjectAuthorizationEvaluationResult authoritative
) {
    public ProjectAuthorizationCoordinationResult {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(legacy);
        target = Objects.requireNonNull(target);
        classification = Objects.requireNonNull(classification);
        Objects.requireNonNull(authoritative);
        if (target.isPresent() != classification.isPresent()) {
            throw new IllegalArgumentException("target과 classification은 함께 존재해야 합니다.");
        }
        if (mode == ProjectAuthorizationRolloutMode.LEGACY && target.isPresent()) {
            throw new IllegalArgumentException("LEGACY mode는 target을 runtime 평가하지 않습니다.");
        }
    }

    public boolean allows() {
        return authoritative.allows();
    }
}
