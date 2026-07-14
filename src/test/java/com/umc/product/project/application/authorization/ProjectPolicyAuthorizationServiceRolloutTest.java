package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationApplicationScope;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassification;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassifier;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationFormView;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationProjectScope;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutCoordinator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutMode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutObserver;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectPolicyAuthorizationServiceRolloutTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    private final ProjectPolicyBundleLoader bundleLoader =
        new ProjectPolicyBundleLoader(new PolicySemanticCompiler());

    @Test
    @DisplayName("LEGACY mode는 legacy decision만 PolicyDecision metadata와 outcome으로 반환한다")
    void legacyModeReturnsLegacyDecisionWithoutTargetEvaluation() {
        AtomicInteger targetCalls = new AtomicInteger();
        AtomicReference<ProjectAuthorizationComparisonRequest> captured = new AtomicReference<>();
        ProjectAuthorizationDecision legacy = new ProjectAuthorizationDecision(
            PolicyEffect.ALLOW,
            new ProjectAuthorizationProjectScope(false, true, Set.of(), Set.of(), Set.of(), false),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE,
            false,
            true,
            List.of()
        );
        ProjectPolicySubjectSnapshot subject = snapshot();
        ProjectPolicyResourceContext context = resource();
        ProjectAuthorizationResourceSnapshot trustedResource =
            ProjectAuthorizationResourceSnapshot.withTargetChapterSchools(context, Set.of(301L));
        ProjectAuthorizationResourceSnapshotFactory resourceSnapshotFactory =
            mock(ProjectAuthorizationResourceSnapshotFactory.class);
        given(resourceSnapshotFactory.create(subject, context, Optional.empty()))
            .willReturn(trustedResource);
        ProjectPolicyAuthorizationService service = new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            new ProjectAuthorizationRolloutCoordinator(
                request -> {
                    captured.set(request);
                    return legacy;
                },
                request -> {
                    targetCalls.incrementAndGet();
                    return ProjectAuthorizationDecision.denied();
                },
                new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
                action -> ProjectAuthorizationRolloutMode.LEGACY
            ),
            bundleLoader,
            resourceSnapshotFactory
        );

        PolicyDecision decision = service.evaluate(subject, ProjectPolicyAction.PROJECT_READ, context);

        assertThat(targetCalls).hasValue(0);
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        verify(resourceSnapshotFactory).create(subject, context, Optional.empty());
        assertThat(captured.get().resourceSnapshot()).isSameAs(trustedResource);
        assertThat(captured.get().resourceSnapshot().targetChapterSchoolIds()).containsExactly(301L);
        assertThat(decision.outcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY))
            .contains(new PolicyValue.BooleanValue(true));
        assertMetadata(decision);
    }

    @Test
    @DisplayName("ENFORCE mode는 동일 request를 양쪽에 한 번씩 전달하고 target을 반환한다")
    void enforceModeReturnsTargetUsingSameRequest() {
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        AtomicReference<ProjectAuthorizationComparisonRequest> captured = new AtomicReference<>();
        ProjectAuthorizationResourceSnapshot resource = ProjectAuthorizationResourceSnapshot.of(resource());
        ProjectAuthorizationEvaluationPoint point = ProjectAuthorizationEvaluationPoint.internal(
            ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        ProjectAuthorizationResourceSnapshotFactory resourceSnapshotFactory =
            mock(ProjectAuthorizationResourceSnapshotFactory.class);
        ProjectPolicyAuthorizationService service = new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            new ProjectAuthorizationRolloutCoordinator(
                request -> {
                    captured.set(request);
                    legacyCalls.incrementAndGet();
                    return ProjectAuthorizationDecision.denied();
                },
                request -> {
                    assertThat(request).isSameAs(captured.get());
                    targetCalls.incrementAndGet();
                    return ProjectAuthorizationDecision.allowed();
                },
                new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
                action -> ProjectAuthorizationRolloutMode.ENFORCE
            ),
            bundleLoader,
            resourceSnapshotFactory
        );

        PolicyDecision decision = service.evaluate(
            snapshot, ProjectPolicyAction.PROJECT_READ, resource, point);

        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(1);
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(captured.get().snapshot()).isSameAs(snapshot);
        assertThat(captured.get().evaluatedAt()).isEqualTo(NOW);
        assertThat(captured.get().resourceSnapshot()).isSameAs(resource);
        assertThat(captured.get().evaluationPoint()).isSameAs(point);
        verifyNoInteractions(resourceSnapshotFactory);
    }

    @Test
    @DisplayName("ENFORCE target failure는 POLICY_EVALUATION_FAILED로 변환한다")
    void targetFailureBecomesAuthorizationDomainException() {
        AtomicReference<ProjectAuthorizationComparisonRequest> captured = new AtomicReference<>();
        ProjectPolicyAuthorizationService service = service(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> {
                captured.set(request);
                return new ProjectAuthorizationEvaluationFailure(
                    ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED, request.evaluatedAt());
            },
            ProjectAuthorizationRolloutMode.ENFORCE
        );

        assertThatThrownBy(() -> service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource()))
            .isInstanceOf(AuthorizationDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        assertThat(captured.get().resourceSnapshot().targetChapterSchoolIds()).isEmpty();
    }

    @Test
    @DisplayName("기존 constructor는 target ENFORCE를 한 번만 평가한다")
    void compatibilityConstructorEvaluatesTargetOnce() {
        AtomicInteger evaluations = new AtomicInteger();
        PolicyDecision target = targetPolicyDecision();
        ProjectPolicyAuthorizationService service = new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            request -> {
                evaluations.incrementAndGet();
                return target;
            },
            bundleLoader
        );

        PolicyDecision decision = service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(evaluations).hasValue(1);
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    @DisplayName("LEGACY 성공은 target 비교 없이 observer를 정확히 한 번 호출한다")
    void legacySuccessObservedOnce() {
        List<ProjectAuthorizationRolloutObserver.Observation> observations = new ArrayList<>();
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectPolicyAuthorizationService service = observedService(
            request -> {
                legacyCalls.incrementAndGet();
                return ProjectAuthorizationDecision.allowed();
            },
            request -> {
                targetCalls.incrementAndGet();
                return ProjectAuthorizationDecision.denied();
            },
            ProjectAuthorizationRolloutMode.LEGACY,
            observations::add
        );

        service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(0);
        assertThat(observations).singleElement().satisfies(observation -> {
            assertThat(observation.mode()).isEqualTo(ProjectAuthorizationRolloutMode.LEGACY);
            assertThat(observation.classification()).isEmpty();
        });
    }

    @Test
    @DisplayName("SHADOW 차이는 legacy authoritative를 유지하며 observer를 정확히 한 번 호출한다")
    void shadowUnexpectedDifferenceObservedOnce() {
        List<ProjectAuthorizationRolloutObserver.Observation> observations = new ArrayList<>();
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectPolicyAuthorizationService service = observedService(
            request -> {
                legacyCalls.incrementAndGet();
                return ProjectAuthorizationDecision.allowed();
            },
            request -> {
                targetCalls.incrementAndGet();
                return ProjectAuthorizationDecision.denied();
            },
            ProjectAuthorizationRolloutMode.SHADOW,
            observations::add
        );

        PolicyDecision decision = service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(1);
        assertThat(observations).singleElement().satisfies(observation -> {
            assertThat(observation.mode()).isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
            assertThat(observation.classification())
                .contains(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE);
        });
    }

    @Test
    @DisplayName("ENFORCE 성공은 target authoritative를 반환하며 observer를 정확히 한 번 호출한다")
    void enforceSuccessObservedOnce() {
        List<ProjectAuthorizationRolloutObserver.Observation> observations = new ArrayList<>();
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectPolicyAuthorizationService service = observedService(
            request -> {
                legacyCalls.incrementAndGet();
                return ProjectAuthorizationDecision.denied();
            },
            request -> {
                targetCalls.incrementAndGet();
                return ProjectAuthorizationDecision.allowed();
            },
            ProjectAuthorizationRolloutMode.ENFORCE,
            observations::add
        );

        PolicyDecision decision = service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(1);
        assertThat(observations).hasSize(1);
    }

    @Test
    @DisplayName("ENFORCE target failure도 observer를 정확히 한 번 호출한 뒤 500으로 변환한다")
    void enforceTargetFailureObservedBeforeThrow() {
        List<ProjectAuthorizationRolloutObserver.Observation> observations = new ArrayList<>();
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        ProjectPolicyAuthorizationService service = observedService(
            request -> {
                legacyCalls.incrementAndGet();
                return new ProjectAuthorizationEvaluationFailure(
                    ProjectAuthorizationEvaluationFailureCode.LEGACY_ADAPTER_FAILED, request.evaluatedAt());
            },
            request -> {
                targetCalls.incrementAndGet();
                return new ProjectAuthorizationEvaluationFailure(
                    ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED, request.evaluatedAt());
            },
            ProjectAuthorizationRolloutMode.ENFORCE,
            observations::add
        );

        assertThatThrownBy(() -> service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource()))
            .isInstanceOf(AuthorizationDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(1);
        assertThat(observations).singleElement().satisfies(observation -> {
            assertThat(observation.bothFailed()).isTrue();
            assertThat(observation.failureCode())
                .contains(ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED);
        });
    }

    @Test
    @DisplayName("observer RuntimeException은 SHADOW legacy ALLOW를 바꾸지 않는다")
    void observerRuntimeFailurePreservesShadowAllow() {
        ProjectPolicyAuthorizationService service = observedService(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> ProjectAuthorizationDecision.denied(),
            ProjectAuthorizationRolloutMode.SHADOW,
            observation -> {
                throw new IllegalStateException("observer failed");
            }
        );

        PolicyDecision decision = service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    @DisplayName("observer RuntimeException은 ENFORCE target DENY를 ALLOW나 failure로 바꾸지 않는다")
    void observerRuntimeFailurePreservesEnforceDeny() {
        ProjectPolicyAuthorizationService service = observedService(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> ProjectAuthorizationDecision.denied(),
            ProjectAuthorizationRolloutMode.ENFORCE,
            observation -> {
                throw new IllegalStateException("observer failed");
            }
        );

        PolicyDecision decision = service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource());

        assertThat(decision.effect()).isEqualTo(PolicyEffect.DENY);
    }

    @Test
    @DisplayName("observer Error는 authorization 보조 실패로 삼키지 않고 전파한다")
    void observerErrorIsPropagated() {
        AssertionError error = new AssertionError("fatal observer");
        ProjectPolicyAuthorizationService service = observedService(
            request -> ProjectAuthorizationDecision.allowed(),
            request -> ProjectAuthorizationDecision.denied(),
            ProjectAuthorizationRolloutMode.SHADOW,
            observation -> {
                throw error;
            }
        );

        assertThatThrownBy(() -> service.evaluate(snapshot(), ProjectPolicyAction.PROJECT_READ, resource()))
            .isSameAs(error);
    }

    private ProjectPolicyAuthorizationService service(
        ProjectAuthorizationEvaluator legacy,
        ProjectAuthorizationEvaluator target,
        ProjectAuthorizationRolloutMode mode
    ) {
        return new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            new ProjectAuthorizationRolloutCoordinator(
                legacy,
                target,
                new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
                action -> mode
            ),
            bundleLoader
        );
    }

    private ProjectPolicyAuthorizationService observedService(
        ProjectAuthorizationEvaluator legacy,
        ProjectAuthorizationEvaluator target,
        ProjectAuthorizationRolloutMode mode,
        ProjectAuthorizationRolloutObserver observer
    ) {
        return new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            new ProjectAuthorizationRolloutCoordinator(
                legacy,
                target,
                new ProjectAuthorizationClassifier((request, left, right) -> java.util.Optional.empty()),
                action -> mode
            ),
            bundleLoader,
            mock(ProjectAuthorizationResourceSnapshotFactory.class),
            observer
        );
    }

    private PolicyDecision targetPolicyDecision() {
        var bundle = bundleLoader.compiled().value();
        return new PolicyDecision(
            PolicyEffect.ALLOW, List.of(), List.of(), List.of(), NOW,
            bundle.schemaVersion(), bundle.contextSchemaVersion(), bundle.policyVersion(), bundle.policyFingerprint());
    }

    private void assertMetadata(PolicyDecision decision) {
        var bundle = bundleLoader.compiled().value();
        assertThat(decision.evaluatedAt()).isEqualTo(NOW);
        assertThat(decision.schemaVersion()).isEqualTo(bundle.schemaVersion());
        assertThat(decision.contextSchemaVersion()).isEqualTo(bundle.contextSchemaVersion());
        assertThat(decision.policyVersion()).isEqualTo(bundle.policyVersion());
        assertThat(decision.policyFingerprint()).isEqualTo(bundle.policyFingerprint());
    }

    private ProjectPolicySubjectSnapshot snapshot() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), NOW, List.of(), List.of(), Map.of());
    }

    private ProjectPolicyResourceContext resource() {
        return ProjectPolicyResourceContext.builder()
            .project(100L, 1L, 10L, ProjectStatus.IN_PROGRESS)
            .build();
    }
}
