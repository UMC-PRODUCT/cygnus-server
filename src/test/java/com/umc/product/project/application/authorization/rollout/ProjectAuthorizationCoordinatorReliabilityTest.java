package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;

class ProjectAuthorizationCoordinatorReliabilityTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    @DisplayName("동일 request의 32개 concurrent 평가는 결정적이고 양쪽을 정확히 한 번씩 호출한다")
    void concurrentEvaluationIsDeterministic() throws Exception {
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectAuthorizationComparisonRequest request = request();
        ProjectAuthorizationRolloutCoordinator coordinator = coordinator(
            actual -> {
                assertThat(actual).isSameAs(request);
                legacyCalls.incrementAndGet();
                return ProjectAuthorizationDecision.allowed();
            },
            actual -> {
                assertThat(actual).isSameAs(request);
                targetCalls.incrementAndGet();
                return ProjectAuthorizationDecision.allowed();
            },
            ProjectAuthorizationRolloutMode.SHADOW
        );
        List<Callable<ProjectAuthorizationCoordinationResult>> tasks = new ArrayList<>();
        for (int index = 0; index < 32; index++) {
            tasks.add(() -> coordinator.coordinate(request));
        }

        List<ProjectAuthorizationCoordinationResult> results;
        try (var executor = Executors.newFixedThreadPool(8)) {
            results = executor.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        }

        assertThat(results).hasSize(32).allMatch(results.getFirst()::equals);
        assertThat(legacyCalls).hasValue(32);
        assertThat(targetCalls).hasValue(32);
    }

    @Test
    @DisplayName("RuntimeException은 typed target failure로 변환하고 ENFORCE를 fail-closed한다")
    void runtimeExceptionBecomesTypedTargetFailure() {
        ProjectAuthorizationRolloutCoordinator coordinator = coordinator(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> {
                throw new IllegalStateException("target failed");
            },
            ProjectAuthorizationRolloutMode.ENFORCE
        );

        ProjectAuthorizationCoordinationResult result = coordinator.coordinate(request());

        assertThat(result.classification()).contains(ProjectAuthorizationClassification.TARGET_FAILURE);
        assertThat(result.target()).contains(new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED, NOW));
        assertThat(result.allows()).isFalse();
    }

    @Test
    @DisplayName("Error는 typed failure로 삼키지 않고 호출자에게 전파한다")
    void errorIsPropagated() {
        AssertionError error = new AssertionError("fatal");
        ProjectAuthorizationRolloutCoordinator coordinator = coordinator(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> {
                throw error;
            },
            ProjectAuthorizationRolloutMode.SHADOW
        );

        assertThatThrownBy(() -> coordinator.coordinate(request())).isSameAs(error);
    }

    @Test
    @DisplayName("호출 전 설정된 thread interrupt flag를 지우지 않는다")
    void interruptFlagIsPreserved() {
        ProjectAuthorizationRolloutCoordinator coordinator = coordinator(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> ProjectAuthorizationDecision.allowed(),
            ProjectAuthorizationRolloutMode.LEGACY
        );

        Thread.currentThread().interrupt();
        try {
            coordinator.coordinate(request());
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private ProjectAuthorizationRolloutCoordinator coordinator(
        ProjectAuthorizationEvaluator legacy,
        ProjectAuthorizationEvaluator target,
        ProjectAuthorizationRolloutMode mode
    ) {
        return new ProjectAuthorizationRolloutCoordinator(
            legacy,
            target,
            new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
            action -> mode
        );
    }

    private ProjectAuthorizationComparisonRequest request() {
        return new ProjectAuthorizationComparisonRequest(
            new ProjectPolicySubjectSnapshot(
                new ProjectPolicyPrincipal.Member(1L), NOW, List.of(), List.of(), Map.of()),
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder().build(),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE)
        );
    }
}
