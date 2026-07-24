package com.umc.product.authorization.application.service.policy.rollout;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.policy.PolicyBundleIdentity;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutClassification;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public interface PolicyRolloutObserver {

    void observe(Observation observation);

    static PolicyRolloutObserver noOp() {
        return observation -> {
        };
    }

    enum EvaluationEffect {
        ALLOW,
        DENY,
        FAILURE,
        NOT_EVALUATED
    }

    record Observation(
        PolicyRolloutKey key,
        PolicyRolloutMode mode,
        Optional<PolicyRolloutClassification> classification,
        EvaluationEffect legacyEffect,
        EvaluationEffect targetEffect,
        PolicyBundleIdentity targetIdentity,
        Instant evaluatedAt,
        long elapsedNanos
    ) {

        public Observation {
            Objects.requireNonNull(key);
            Objects.requireNonNull(mode);
            classification = Objects.requireNonNull(classification);
            Objects.requireNonNull(legacyEffect);
            Objects.requireNonNull(targetEffect);
            Objects.requireNonNull(targetIdentity);
            Objects.requireNonNull(evaluatedAt);
            if (legacyEffect == EvaluationEffect.NOT_EVALUATED) {
                throw new IllegalArgumentException("legacy effect는 항상 평가되어야 합니다.");
            }
            if (mode == PolicyRolloutMode.LEGACY
                && (targetEffect != EvaluationEffect.NOT_EVALUATED || classification.isPresent())) {
                throw new IllegalArgumentException("LEGACY mode는 target 결과를 기록할 수 없습니다.");
            }
            if (mode != PolicyRolloutMode.LEGACY
                && (targetEffect == EvaluationEffect.NOT_EVALUATED || classification.isEmpty())) {
                throw new IllegalArgumentException("SHADOW/ENFORCE mode는 target 결과와 분류가 필요합니다.");
            }
            if (elapsedNanos < 0) {
                throw new IllegalArgumentException("Policy 평가 시간은 음수일 수 없습니다.");
            }
        }
    }

    @Component
    final class MicrometerObserver implements PolicyRolloutObserver {

        private static final Logger log = LoggerFactory.getLogger("authorization_policy");
        private static final String DECISION_COUNTER = "authorization.policy.decision.total";
        private static final String EVALUATION_TIMER = "authorization.policy.evaluation.seconds";
        private static final String EVENT = "authorization_policy_evaluated";

        private final MeterRegistry registry;

        public MicrometerObserver(MeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void observe(Observation observation) {
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
                kv("namespace", observation.key().namespace()),
                kv("action", observation.key().actionId()),
                kv("mode", observation.mode().name()),
                kv("classification", classification(observation)),
                kv("legacyEffect", observation.legacyEffect().name()),
                kv("targetEffect", observation.targetEffect().name()),
                kv("schemaVersion", observation.targetIdentity().schemaVersion()),
                kv("contextSchemaVersion", observation.targetIdentity().contextSchemaVersion()),
                kv("policyVersion", observation.targetIdentity().policyVersion()),
                kv("policyFingerprint", observation.targetIdentity().policyFingerprint()),
                kv("evaluatedAt", observation.evaluatedAt().toString())
            );
        }

        private Tags tags(Observation observation) {
            return Tags.of(
                "namespace", observation.key().namespace(),
                "action", observation.key().actionId(),
                "mode", observation.mode().name(),
                "classification", classification(observation),
                "schemaVersion", observation.targetIdentity().schemaVersion(),
                "contextSchemaVersion", observation.targetIdentity().contextSchemaVersion(),
                "policyVersion", observation.targetIdentity().policyVersion(),
                "policyFingerprint", observation.targetIdentity().policyFingerprint());
        }

        private String classification(Observation observation) {
            return observation.classification()
                .map(PolicyRolloutClassification::name)
                .orElse("NOT_COMPARED");
        }
    }
}
