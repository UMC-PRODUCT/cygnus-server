package com.umc.product.project.application.authorization.rollout;

import java.time.Instant;
import java.util.Objects;

public record ProjectAuthorizationEvaluationFailure(
    ProjectAuthorizationEvaluationFailureCode failureCode,
    Instant evaluatedAt
) implements ProjectAuthorizationEvaluationResult {
    public ProjectAuthorizationEvaluationFailure {
        Objects.requireNonNull(failureCode);
        Objects.requireNonNull(evaluatedAt);
    }

    @Override
    public boolean allows() {
        return false;
    }
}
