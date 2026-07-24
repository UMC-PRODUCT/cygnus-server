package com.umc.product.authorization.application.service.policy.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutClassification;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

class PolicyRolloutCoordinatorTest {

    private static final PolicyRolloutKey KEY = new PolicyRolloutKey("term", "term:create");

    @Test
    @DisplayName("LEGACY는 target을 평가하지 않고 legacy 결과를 authoritative로 사용한다")
    void legacySkipsTargetEvaluation() {
        AtomicInteger targetCalls = new AtomicInteger();
        var coordinator = new PolicyRolloutCoordinator<String, Boolean>(
            context -> success(true),
            context -> {
                targetCalls.incrementAndGet();
                return success(false);
            },
            PolicyExpectedDifference.none(),
            key -> PolicyRolloutMode.LEGACY);

        var result = coordinator.coordinate(KEY, "context");

        assertThat(result.authoritative()).isEqualTo(success(true));
        assertThat(result.target()).isEmpty();
        assertThat(result.classification()).isEmpty();
        assertThat(targetCalls).hasValue(0);
    }

    @Test
    @DisplayName("SHADOW는 양쪽을 비교하지만 legacy 결과를 authoritative로 사용한다")
    void shadowComparesAndKeepsLegacyAuthoritative() {
        var coordinator = new PolicyRolloutCoordinator<String, Boolean>(
            context -> success(true),
            context -> success(false),
            PolicyExpectedDifference.none(),
            key -> PolicyRolloutMode.SHADOW);

        var result = coordinator.coordinate(KEY, "context");

        assertThat(result.authoritative()).isEqualTo(success(true));
        assertThat(result.classification())
            .contains(PolicyRolloutClassification.UNEXPECTED_DIFFERENCE);
    }

    @Test
    @DisplayName("ENFORCE는 target failure를 legacy allow로 fallback하지 않는다")
    void enforceDoesNotFallbackOnTargetFailure() {
        var coordinator = new PolicyRolloutCoordinator<String, Boolean>(
            context -> success(true),
            context -> {
                throw new IllegalStateException("broken target");
            },
            PolicyExpectedDifference.none(),
            key -> PolicyRolloutMode.ENFORCE);

        var result = coordinator.coordinate(KEY, "context");

        assertThat(result.authoritative())
            .isEqualTo(new PolicyRolloutEvaluation.Failure<Boolean>("POLICY_EVALUATION_FAILED"));
        assertThat(result.classification()).contains(PolicyRolloutClassification.TARGET_FAILURE);
    }

    private PolicyRolloutEvaluation<Boolean> success(boolean decision) {
        return new PolicyRolloutEvaluation.Success<>(decision);
    }
}
