package com.umc.product.project.adapter.in.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ProjectAuthorizationSurfaceObservationAspectTest {

    @Test
    @DisplayName("REST 진입점을 한 번 실행하고 closed surface tag의 counter와 timer를 기록한다")
    void observesRestSurfaceOnce() throws Throwable {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProjectAuthorizationSurfaceObservationAspect aspect =
            new ProjectAuthorizationSurfaceObservationAspect(registry);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Object expected = new Object();
        when(joinPoint.proceed()).thenReturn(expected);
        ProjectAuthorizationSurface surface = ProjectAuthorizationSurface.REST_PROJECT_CREATE;

        // when
        bind(joinPoint, surface);

        Object actual = aspect.observe(joinPoint);

        // then
        assertThat(actual).isSameAs(expected);
        verify(joinPoint, times(1)).proceed();
        assertThat(counter(registry, surface)).isEqualTo(1.0);
        assertThat(timer(registry, surface)).isEqualTo(1L);
    }

    @Test
    @DisplayName("handler 예외를 그대로 전파하면서 호출 counter와 timer를 기록한다")
    void propagatesHandlerFailureAndObservesOnce() throws Throwable {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProjectAuthorizationSurfaceObservationAspect aspect =
            new ProjectAuthorizationSurfaceObservationAspect(registry);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        IllegalStateException expected = new IllegalStateException("handler failure");
        when(joinPoint.proceed()).thenThrow(expected);
        ProjectAuthorizationSurface surface = ProjectAuthorizationSurface.SCHEDULER_MATCHING_ROUND_DEADLINE;

        // when & then
        bind(joinPoint, surface);

        assertThatThrownBy(() -> aspect.observe(joinPoint)).isSameAs(expected);
        verify(joinPoint, times(1)).proceed();
        assertThat(counter(registry, surface)).isEqualTo(1.0);
        assertThat(timer(registry, surface)).isEqualTo(1L);
    }

    @Test
    @DisplayName("GraphQL parent-transitive 진입점은 boundary만 관측하고 handler를 한 번만 실행한다")
    void observesGraphQlParentTransitiveBoundaryWithoutAdditionalEvaluation() throws Throwable {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProjectAuthorizationSurfaceObservationAspect aspect =
            new ProjectAuthorizationSurfaceObservationAspect(registry);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(null);
        ProjectAuthorizationSurface surface = ProjectAuthorizationSurface.GRAPHQL_PROJECT_PRODUCT_OWNER;

        // when
        bind(joinPoint, surface);

        aspect.observe(joinPoint);

        // then
        verify(joinPoint, times(1)).proceed();
        assertThat(counter(registry, surface)).isEqualTo(1.0);
        assertThat(timer(registry, surface)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Micrometer RuntimeException은 handler 실행과 반환값을 바꾸지 않는다")
    void micrometerRuntimeFailureDoesNotChangeHandlerResult() throws Throwable {
        ProjectAuthorizationSurfaceObservationAspect aspect =
            new ProjectAuthorizationSurfaceObservationAspect(mock(MeterRegistry.class));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Object expected = new Object();
        when(joinPoint.proceed()).thenReturn(expected);
        bind(joinPoint, ProjectAuthorizationSurface.REST_PROJECT_CREATE);

        Object actual = aspect.observe(joinPoint);

        assertThat(actual).isSameAs(expected);
        verify(joinPoint, times(1)).proceed();
    }

    private void bind(ProceedingJoinPoint joinPoint, ProjectAuthorizationSurface surface)
        throws NoSuchMethodException {
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(AnnotatedHandlers.class.getDeclaredMethod(surface.name()));
    }

    private static final class AnnotatedHandlers {
        @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_CREATE)
        private void REST_PROJECT_CREATE() {}

        @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.SCHEDULER_MATCHING_ROUND_DEADLINE)
        private void SCHEDULER_MATCHING_ROUND_DEADLINE() {}

        @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.GRAPHQL_PROJECT_PRODUCT_OWNER)
        private void GRAPHQL_PROJECT_PRODUCT_OWNER() {}
    }

    private double counter(SimpleMeterRegistry registry, ProjectAuthorizationSurface surface) {
        return registry.get(ProjectAuthorizationSurfaceObservationAspect.CALL_COUNTER)
            .tag("surface", surface.name())
            .tag("action", surface.action().name())
            .tag("interfaceType", surface.type().name())
            .counter()
            .count();
    }

    private long timer(SimpleMeterRegistry registry, ProjectAuthorizationSurface surface) {
        return registry.get(ProjectAuthorizationSurfaceObservationAspect.CALL_TIMER)
            .tag("surface", surface.name())
            .tag("action", surface.action().name())
            .tag("interfaceType", surface.type().name())
            .timer()
            .count();
    }
}
