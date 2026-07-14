package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceCatalog;

class ProjectAuthorizationCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    @DisplayName("closed surface enum은 catalog 46개 identity와 exact-set으로 일치한다")
    void closedSurfaceEnumMatchesCatalogExactly() {
        Set<String> typed = Arrays.stream(ProjectAuthorizationSurface.values())
            .map(ProjectAuthorizationSurface::id)
            .collect(java.util.stream.Collectors.toSet());
        Set<String> catalog = ProjectPolicySurfaceCatalog.surfaces().stream()
            .map(surface -> surface.id())
            .collect(java.util.stream.Collectors.toSet());

        assertThat(typed).hasSize(46).isEqualTo(catalog);
    }

    @Test
    @DisplayName("target failure는 양쪽 failure에서도 우선 분류한다")
    void targetFailureHasPrecedenceWhenBothFail() {
        ProjectAuthorizationClassifier classifier = new ProjectAuthorizationClassifier((request, legacy, target) ->
            java.util.Optional.empty());

        ProjectAuthorizationClassification classification = classifier.classify(
            request(), failure(), failure());

        assertThat(classification).isEqualTo(ProjectAuthorizationClassification.TARGET_FAILURE);
    }

    @Test
    @DisplayName("coordinator는 동일 request를 양쪽에 한 번씩 평가하고 ENFORCE에서 target을 선택한다")
    void coordinatorEvaluatesBothOnceAndSelectsTargetInEnforce() {
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectAuthorizationComparisonRequest request = request();
        ProjectAuthorizationDecision legacy = ProjectAuthorizationDecision.allowed();
        ProjectAuthorizationDecision target = ProjectAuthorizationDecision.denied();
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            actual -> {
                assertThat(actual).isSameAs(request);
                legacyCalls.incrementAndGet();
                return legacy;
            },
            actual -> {
                assertThat(actual).isSameAs(request);
                targetCalls.incrementAndGet();
                return target;
            },
            new ProjectAuthorizationClassifier((actual, left, right) ->
                java.util.Optional.of(ProjectExpectedDifferenceId.E001)),
            action -> ProjectAuthorizationRolloutMode.ENFORCE
        );

        ProjectAuthorizationCoordinationResult result = coordinator.coordinate(request);

        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(1);
        assertThat(result.classification()).contains(ProjectAuthorizationClassification.EXPECTED_DIFFERENCE);
        assertThat(result.target()).containsSame(target);
        assertThat(result.authoritative()).isSameAs(target);
        assertThat(result.allows()).isFalse();
    }

    @Test
    @DisplayName("LEGACY mode는 target을 runtime 평가하지 않고 legacy만 authoritative로 선택한다")
    void legacyModeSkipsTargetRuntimeEvaluation() {
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectAuthorizationDecision legacy = ProjectAuthorizationDecision.allowed();
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            request -> legacy,
            request -> {
                targetCalls.incrementAndGet();
                return ProjectAuthorizationDecision.denied();
            },
            new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
            action -> ProjectAuthorizationRolloutMode.LEGACY
        );

        ProjectAuthorizationCoordinationResult result = coordinator.coordinate(request());

        assertThat(targetCalls).hasValue(0);
        assertThat(result.target()).isEmpty();
        assertThat(result.classification()).isEmpty();
        assertThat(result.authoritative()).isSameAs(legacy);
        assertThat(result.allows()).isTrue();
    }

    @Test
    @DisplayName("classifier RuntimeException은 SHADOW legacy authoritative 결정을 바꾸지 않는다")
    void classifierRuntimeFailurePreservesShadowAuthoritativeDecision() {
        ProjectAuthorizationDecision legacy = ProjectAuthorizationDecision.allowed();
        ProjectAuthorizationDecision target = ProjectAuthorizationDecision.denied();
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            request -> legacy,
            request -> target,
            new ProjectAuthorizationClassifier((request, left, right) -> {
                throw new IllegalStateException("classifier failed");
            }),
            action -> ProjectAuthorizationRolloutMode.SHADOW
        );

        ProjectAuthorizationCoordinationResult result = coordinator.coordinate(request());

        assertThat(result.authoritative()).isSameAs(legacy);
        assertThat(result.allows()).isTrue();
        assertThat(result.classification())
            .contains(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE);
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

    private ProjectAuthorizationEvaluationFailure failure() {
        return new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED, NOW);
    }
}
