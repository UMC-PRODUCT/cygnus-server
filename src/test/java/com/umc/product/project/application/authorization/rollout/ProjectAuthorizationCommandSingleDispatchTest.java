package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;

class ProjectAuthorizationCommandSingleDispatchTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @ParameterizedTest(name = "{0}")
    @MethodSource("dispatchCases")
    @DisplayName("authoritative decision에 따라 command와 outbox를 각각 최대 한 번 실행한다")
    void dispatchesCommandAndOutboxAtMostOnce(DispatchCase dispatchCase) {
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        AtomicInteger commandCalls = new AtomicInteger();
        AtomicInteger outboxCalls = new AtomicInteger();
        ProjectAuthorizationRolloutCoordinator coordinator = coordinator(
            dispatchCase.configuration(),
            request -> {
                legacyCalls.incrementAndGet();
                return dispatchCase.legacy();
            },
            request -> {
                targetCalls.incrementAndGet();
                return dispatchCase.target();
            }
        );

        ProjectAuthorizationCoordinationResult result = dispatch(coordinator, request(), () -> {
            commandCalls.incrementAndGet();
            outboxCalls.incrementAndGet();
        });

        assertThat(result.mode()).isEqualTo(dispatchCase.expectedMode());
        assertThat(result.classification()).isEqualTo(dispatchCase.expectedClassification());
        assertThat(result.allows()).isEqualTo(dispatchCase.expectedExecutions() == 1);
        assertThat(legacyCalls).hasValue(1);
        assertThat(targetCalls).hasValue(dispatchCase.expectedTargetCalls());
        assertThat(commandCalls).hasValue(dispatchCase.expectedExecutions());
        assertThat(outboxCalls).hasValue(dispatchCase.expectedExecutions());
    }

    @Test
    @DisplayName("coordinator는 command, save, repository, mutator callback에 의존하지 않는다")
    void coordinatorDoesNotDependOnSideEffects() {
        List<Class<?>> fieldTypes = Arrays.stream(ProjectAuthorizationRolloutCoordinator.class.getDeclaredFields())
            .map(Field::getType)
            .toList();
        List<Class<?>> parameterTypes = Arrays.stream(ProjectAuthorizationRolloutCoordinator.class.getDeclaredMethods())
            .flatMap(method -> Arrays.stream(method.getParameterTypes()))
            .toList();

        assertThat(fieldTypes).containsOnly(
            ProjectAuthorizationEvaluator.class,
            ProjectAuthorizationClassifier.class,
            ProjectAuthorizationRolloutModeResolver.class
        );
        assertThat(parameterTypes).doesNotContain(Runnable.class);
        assertThat(fieldTypes).allMatch(type -> !hasSideEffectName(type));
    }

    private static Stream<Arguments> dispatchCases() {
        return Stream.of(
            arguments("LEGACY allow", mode(ProjectAuthorizationRolloutMode.LEGACY), allow(), deny(),
                ProjectAuthorizationRolloutMode.LEGACY, Optional.empty(), 0, 1),
            arguments("LEGACY deny", mode(ProjectAuthorizationRolloutMode.LEGACY), deny(), allow(),
                ProjectAuthorizationRolloutMode.LEGACY, Optional.empty(), 0, 0),
            arguments("LEGACY failure", mode(ProjectAuthorizationRolloutMode.LEGACY), failure(), allow(),
                ProjectAuthorizationRolloutMode.LEGACY, Optional.empty(), 0, 0),
            arguments("SHADOW match allow", mode(ProjectAuthorizationRolloutMode.SHADOW), allow(), allow(),
                ProjectAuthorizationRolloutMode.SHADOW, Optional.of(ProjectAuthorizationClassification.MATCH), 1, 1),
            arguments("SHADOW unexpected legacy allow target deny", mode(ProjectAuthorizationRolloutMode.SHADOW),
                allow(), deny(), ProjectAuthorizationRolloutMode.SHADOW,
                Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE), 1, 1),
            arguments("SHADOW target failure legacy allow", mode(ProjectAuthorizationRolloutMode.SHADOW),
                allow(), failure(), ProjectAuthorizationRolloutMode.SHADOW,
                Optional.of(ProjectAuthorizationClassification.TARGET_FAILURE), 1, 1),
            arguments("SHADOW legacy failure target allow", mode(ProjectAuthorizationRolloutMode.SHADOW),
                failure(), allow(), ProjectAuthorizationRolloutMode.SHADOW,
                Optional.of(ProjectAuthorizationClassification.LEGACY_FAILURE), 1, 0),
            arguments("ENFORCE unexpected legacy deny target allow", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                deny(), allow(), ProjectAuthorizationRolloutMode.ENFORCE,
                Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE), 1, 1),
            arguments("ENFORCE target deny", mode(ProjectAuthorizationRolloutMode.ENFORCE), allow(), deny(),
                ProjectAuthorizationRolloutMode.ENFORCE,
                Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE), 1, 0),
            arguments("ENFORCE target failure", mode(ProjectAuthorizationRolloutMode.ENFORCE), allow(), failure(),
                ProjectAuthorizationRolloutMode.ENFORCE,
                Optional.of(ProjectAuthorizationClassification.TARGET_FAILURE), 1, 0),
            arguments("ENFORCE legacy failure target allow", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                failure(), allow(), ProjectAuthorizationRolloutMode.ENFORCE,
                Optional.of(ProjectAuthorizationClassification.LEGACY_FAILURE), 1, 1),
            arguments("SHADOW both failure", mode(ProjectAuthorizationRolloutMode.SHADOW), failure(), failure(),
                ProjectAuthorizationRolloutMode.SHADOW,
                Optional.of(ProjectAuthorizationClassification.TARGET_FAILURE), 1, 0),
            arguments("override LEGACY over ENFORCE", override(ProjectAuthorizationRolloutMode.ENFORCE,
                ProjectAuthorizationRolloutMode.LEGACY), allow(), deny(), ProjectAuthorizationRolloutMode.LEGACY,
                Optional.empty(), 0, 1),
            arguments("override ENFORCE over LEGACY", override(ProjectAuthorizationRolloutMode.LEGACY,
                ProjectAuthorizationRolloutMode.ENFORCE), deny(), allow(), ProjectAuthorizationRolloutMode.ENFORCE,
                Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE), 1, 1)
        );
    }

    private static Arguments arguments(
        String name,
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target,
        ProjectAuthorizationRolloutMode expectedMode,
        Optional<ProjectAuthorizationClassification> expectedClassification,
        int expectedTargetCalls,
        int expectedExecutions
    ) {
        return Arguments.of(new DispatchCase(name, configuration, legacy, target, expectedMode,
            expectedClassification, expectedTargetCalls, expectedExecutions));
    }

    private ProjectAuthorizationRolloutCoordinator coordinator(
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationEvaluator legacy,
        ProjectAuthorizationEvaluator target
    ) {
        ProjectAuthorizationRolloutModeResolver modeResolver =
            new ConfiguredProjectAuthorizationRolloutModeResolver(configuration);
        return new ProjectAuthorizationRolloutCoordinator(
            legacy,
            target,
            new ProjectAuthorizationClassifier((request, left, right) -> Optional.empty()),
            modeResolver
        );
    }

    private ProjectAuthorizationCoordinationResult dispatch(
        ProjectAuthorizationRolloutCoordinator coordinator,
        ProjectAuthorizationComparisonRequest request,
        Runnable command
    ) {
        ProjectAuthorizationCoordinationResult result = coordinator.coordinate(request);
        if (result.allows()) {
            command.run();
        }
        return result;
    }

    private boolean hasSideEffectName(Class<?> type) {
        String simpleName = type.getSimpleName();
        return simpleName.contains("Command")
            || simpleName.contains("Save")
            || simpleName.contains("Repository")
            || simpleName.contains("Mutator");
    }

    private ProjectAuthorizationComparisonRequest request() {
        return new ProjectAuthorizationComparisonRequest(
            new ProjectPolicySubjectSnapshot(
                new ProjectPolicyPrincipal.Member(1L), NOW, List.of(), List.of(), Map.of()),
            ProjectPolicyAction.PROJECT_PUBLISH,
            ProjectPolicyResourceContext.builder().build(),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE)
        );
    }

    private static ProjectAuthorizationRolloutConfiguration mode(ProjectAuthorizationRolloutMode mode) {
        return new ProjectAuthorizationRolloutConfiguration(mode, Map.of());
    }

    private static ProjectAuthorizationRolloutConfiguration override(
        ProjectAuthorizationRolloutMode defaultMode,
        ProjectAuthorizationRolloutMode overrideMode
    ) {
        return new ProjectAuthorizationRolloutConfiguration(
            defaultMode, Map.of(ProjectPolicyAction.PROJECT_PUBLISH, overrideMode));
    }

    private static ProjectAuthorizationDecision allow() {
        return ProjectAuthorizationDecision.allowed();
    }

    private static ProjectAuthorizationDecision deny() {
        return ProjectAuthorizationDecision.denied();
    }

    private static ProjectAuthorizationEvaluationFailure failure() {
        return new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED, NOW);
    }

    private record DispatchCase(
        String name,
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target,
        ProjectAuthorizationRolloutMode expectedMode,
        Optional<ProjectAuthorizationClassification> expectedClassification,
        int expectedTargetCalls,
        int expectedExecutions
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
