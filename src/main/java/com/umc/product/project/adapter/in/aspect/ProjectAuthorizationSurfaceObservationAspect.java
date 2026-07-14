package com.umc.product.project.adapter.in.aspect;

import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationAuxiliaryFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationAuxiliaryFailureReporter;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProjectAuthorizationSurfaceObservationAspect {

    static final String CALL_COUNTER = "project.authorization.surface.calls.total";
    static final String CALL_TIMER = "project.authorization.surface.duration";

    private final MeterRegistry registry;

    public ProjectAuthorizationSurfaceObservationAspect(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    @Around("@annotation(com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding)")
    public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ProjectAuthorizationSurfaceBinding binding = signature.getMethod()
            .getDeclaredAnnotation(ProjectAuthorizationSurfaceBinding.class);
        ProjectAuthorizationSurface surface = binding.value();
        Tags tags = tags(surface);
        incrementCounter(surface, tags);
        Timer.Sample sample = startTimer(surface);
        try {
            return joinPoint.proceed();
        } finally {
            stopTimer(surface, tags, sample);
        }
    }

    private void incrementCounter(ProjectAuthorizationSurface surface, Tags tags) {
        try {
            Counter.builder(CALL_COUNTER).tags(tags).register(registry).increment();
        } catch (RuntimeException exception) {
            report(ProjectAuthorizationAuxiliaryFailureCode.SURFACE_COUNTER_FAILED, surface);
        }
    }

    private Timer.Sample startTimer(ProjectAuthorizationSurface surface) {
        try {
            return Timer.start(registry);
        } catch (RuntimeException exception) {
            report(ProjectAuthorizationAuxiliaryFailureCode.SURFACE_TIMER_START_FAILED, surface);
            return null;
        }
    }

    private void stopTimer(ProjectAuthorizationSurface surface, Tags tags, Timer.Sample sample) {
        if (sample == null) {
            return;
        }
        try {
            sample.stop(Timer.builder(CALL_TIMER).tags(tags).register(registry));
        } catch (RuntimeException exception) {
            report(ProjectAuthorizationAuxiliaryFailureCode.SURFACE_TIMER_STOP_FAILED, surface);
        }
    }

    private void report(
        ProjectAuthorizationAuxiliaryFailureCode failureCode,
        ProjectAuthorizationSurface surface
    ) {
        ProjectAuthorizationAuxiliaryFailureReporter.warn(failureCode, surface.action());
    }

    private static Tags tags(ProjectAuthorizationSurface surface) {
        return Tags.of(
            "surface", surface.name(),
            "action", surface.action().name(),
            "interfaceType", surface.type().name()
        );
    }
}
