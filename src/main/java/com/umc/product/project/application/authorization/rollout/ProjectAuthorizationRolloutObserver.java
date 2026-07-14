package com.umc.product.project.application.authorization.rollout;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public interface ProjectAuthorizationRolloutObserver {

    void observe(Observation observation);

    static ProjectAuthorizationRolloutObserver noOp() {
        return observation -> { };
    }

    enum EvaluationEffect {
        ALLOW,
        DENY,
        FAILURE,
        NOT_EVALUATED
    }

    enum TargetPrivilegeExpansion {
        YES,
        NO,
        INDETERMINATE,
        NOT_EVALUATED
    }

    record Observation(
        ProjectPolicyAction action,
        ProjectAuthorizationRolloutMode mode,
        Optional<ProjectAuthorizationClassification> classification,
        ProjectAuthorizationEvaluationPoint evaluationPoint,
        boolean bothFailed,
        Optional<ProjectAuthorizationEvaluationFailureCode> failureCode,
        EvaluationEffect legacyEffect,
        EvaluationEffect targetEffect,
        TargetPrivilegeExpansion targetPrivilegeExpansion,
        Instant evaluatedAt,
        String schemaVersion,
        String contextSchemaVersion,
        String policyVersion,
        String policyFingerprint,
        long elapsedNanos
    ) {
        public Observation {
            Objects.requireNonNull(action);
            Objects.requireNonNull(mode);
            classification = Objects.requireNonNull(classification);
            Objects.requireNonNull(evaluationPoint);
            failureCode = Objects.requireNonNull(failureCode);
            Objects.requireNonNull(legacyEffect);
            Objects.requireNonNull(targetEffect);
            Objects.requireNonNull(targetPrivilegeExpansion);
            Objects.requireNonNull(evaluatedAt);
            Objects.requireNonNull(schemaVersion);
            Objects.requireNonNull(contextSchemaVersion);
            Objects.requireNonNull(policyVersion);
            Objects.requireNonNull(policyFingerprint);
            if (mode == ProjectAuthorizationRolloutMode.LEGACY && classification.isPresent()) {
                throw new IllegalArgumentException("LEGACY mode는 target 비교 classification을 가질 수 없습니다.");
            }
            if (legacyEffect == EvaluationEffect.NOT_EVALUATED) {
                throw new IllegalArgumentException("legacy effect는 항상 평가되어야 합니다.");
            }
            if (mode == ProjectAuthorizationRolloutMode.LEGACY
                && (targetEffect != EvaluationEffect.NOT_EVALUATED
                    || targetPrivilegeExpansion != TargetPrivilegeExpansion.NOT_EVALUATED)) {
                throw new IllegalArgumentException("LEGACY mode는 target 결과를 기록할 수 없습니다.");
            }
            if (mode != ProjectAuthorizationRolloutMode.LEGACY
                && targetEffect == EvaluationEffect.NOT_EVALUATED) {
                throw new IllegalArgumentException("SHADOW/ENFORCE mode는 target effect가 필요합니다.");
            }
            if (mode != ProjectAuthorizationRolloutMode.LEGACY && classification.isEmpty()) {
                throw new IllegalArgumentException("SHADOW/ENFORCE mode는 classification이 필요합니다.");
            }
            if (bothFailed && failureCode.isEmpty()) {
                throw new IllegalArgumentException("양쪽 평가 실패에는 failure code가 필요합니다.");
            }
            if (elapsedNanos < 0) {
                throw new IllegalArgumentException("authorization 평가 시간은 음수일 수 없습니다.");
            }
        }
    }

    @Component
    final class MicrometerObserver implements ProjectAuthorizationRolloutObserver {
        private static final Logger log = LoggerFactory.getLogger("project_authorization");
        private static final String DECISION_COUNTER = "project.authorization.decision.total";
        private static final String EVALUATION_TIMER = "project.authorization.evaluation.seconds";
        private static final String EVENT = "project_authorization_evaluated";

        private final MeterRegistry registry;

        public MicrometerObserver(MeterRegistry registry) {
            this.registry = Objects.requireNonNull(registry);
        }

        @Override
        public void observe(Observation observation) {
            Objects.requireNonNull(observation);
            Tags tags = tags(observation);
            Counter.builder(DECISION_COUNTER)
                .tags(tags)
                .register(registry)
                .increment();
            Timer.builder(EVALUATION_TIMER)
                .tags(tags)
                .publishPercentiles(0.95)
                .publishPercentileHistogram()
                .register(registry)
                .record(observation.elapsedNanos(), TimeUnit.NANOSECONDS);
            log.info(
                EVENT,
                kv("action", observation.action().id()),
                kv("mode", observation.mode().name()),
                kv("classification", classification(observation)),
                kv("schemaVersion", observation.schemaVersion()),
                kv("contextSchemaVersion", observation.contextSchemaVersion()),
                kv("policyVersion", observation.policyVersion()),
                kv("policyFingerprint", observation.policyFingerprint()),
                kv("evaluationPoint", evaluationPoint(observation.evaluationPoint())),
                kv("legacyEffect", observation.legacyEffect().name()),
                kv("targetEffect", observation.targetEffect().name()),
                kv("targetPrivilegeExpansion", observation.targetPrivilegeExpansion().name()),
                kv("evaluatedAt", observation.evaluatedAt().toString()),
                kv("bothFailed", observation.bothFailed()),
                kv("failureCode", failureCode(observation))
            );
        }

        private static Tags tags(Observation observation) {
            return Tags.of(
                "action", observation.action().id(),
                "mode", observation.mode().name(),
                "classification", classification(observation),
                "schemaVersion", observation.schemaVersion(),
                "contextSchemaVersion", observation.contextSchemaVersion(),
                "policyVersion", observation.policyVersion(),
                "policyFingerprint", observation.policyFingerprint(),
                "evaluationPoint", evaluationPoint(observation.evaluationPoint())
            );
        }

        private static String classification(Observation observation) {
            return observation.classification()
                .map(ProjectAuthorizationClassification::name)
                .orElse("NOT_COMPARED");
        }

        private static String evaluationPoint(ProjectAuthorizationEvaluationPoint evaluationPoint) {
            return switch (evaluationPoint) {
                case ProjectAuthorizationEvaluationPoint.Surface surface ->
                    "SURFACE:" + surface.surface().name();
                case ProjectAuthorizationEvaluationPoint.Internal internal ->
                    "INTERNAL:" + internal.origin().name();
            };
        }

        private static String failureCode(Observation observation) {
            return observation.failureCode()
                .map(ProjectAuthorizationEvaluationFailureCode::name)
                .orElse("NONE");
        }
    }
}
