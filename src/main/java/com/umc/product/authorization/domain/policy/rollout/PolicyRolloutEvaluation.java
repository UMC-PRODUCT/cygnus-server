package com.umc.product.authorization.domain.policy.rollout;

import java.util.Objects;

public sealed interface PolicyRolloutEvaluation<D>
    permits PolicyRolloutEvaluation.Success, PolicyRolloutEvaluation.Failure {

    record Success<D>(D decision) implements PolicyRolloutEvaluation<D> {

        public Success {
            Objects.requireNonNull(decision);
        }
    }

    record Failure<D>(String failureCode) implements PolicyRolloutEvaluation<D> {

        public Failure {
            Objects.requireNonNull(failureCode);
            if (failureCode.isBlank()) {
                throw new IllegalArgumentException("Policy rollout failure code는 비어 있을 수 없습니다.");
            }
        }
    }
}
