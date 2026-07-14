package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import net.logstash.logback.argument.StructuredArgument;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ProjectAuthorizationRolloutObserverTest {

    private SimpleMeterRegistry registry;
    private ProjectAuthorizationRolloutObserver observer;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        observer = new ProjectAuthorizationRolloutObserver.MicrometerObserver(registry);
        logger = (Logger) LoggerFactory.getLogger("project_authorization");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        registry.close();
    }

    @Test
    @DisplayName("LEGACY 평가는 비교하지 않았다는 고정 classification으로 집계한다")
    void legacyClassification() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.LEGACY,
            Optional.empty(),
            false,
            Optional.empty()
        ));

        assertThat(registry.get("project.authorization.decision.total")
            .tag("classification", "NOT_COMPARED")
            .counter()
            .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("decision counter는 허용된 여덟 개의 bounded tag만 가진다")
    void boundedTags() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.SHADOW,
            Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE),
            false,
            Optional.empty()
        ));

        Meter meter = registry.get("project.authorization.decision.total").counter();
        assertThat(meter.getId().getTags())
            .extracting(tag -> tag.getKey())
            .containsExactlyInAnyOrder(
                "action",
                "mode",
                "classification",
                "schemaVersion",
                "contextSchemaVersion",
                "policyVersion",
                "policyFingerprint",
                "evaluationPoint"
            );
    }

    @Test
    @DisplayName("평가 시간 timer는 histogram과 p95 조회값을 제공한다")
    void timerHistogramAndP95() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.SHADOW,
            Optional.of(ProjectAuthorizationClassification.MATCH),
            false,
            Optional.empty()
        ));

        Timer timer = registry.get("project.authorization.evaluation.seconds").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isEqualTo(25_000_000L);
        assertThat(timer.takeSnapshot().percentileValues())
            .anySatisfy(value -> assertThat(value.percentile()).isEqualTo(0.95));
    }

    @Test
    @DisplayName("실패 정보는 meter tag가 아니라 PII 없는 구조화 로그에만 기록한다")
    void failureLogOnly() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.ENFORCE,
            Optional.of(ProjectAuthorizationClassification.TARGET_FAILURE),
            true,
            Optional.of(ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED)
        ));

        ILoggingEvent event = onlyEvent();
        assertThat(event.getMessage()).isEqualTo("project_authorization_evaluated");
        assertThat(structuredValue(event, "bothFailed")).isEqualTo("true");
        assertThat(structuredValue(event, "failureCode")).isEqualTo("POLICY_EVALUATION_FAILED");
        assertThat(structuredValue(event, "evaluatedAt")).isEqualTo("2026-07-14T00:00:00Z");
        assertThat(registry.getMeters())
            .flatExtracting(meter -> meter.getId().getTags())
            .extracting(tag -> tag.getKey())
            .doesNotContain("bothFailed", "failureCode", "evaluatedAt");
    }

    @Test
    @DisplayName("target 권한 확대 여부는 bounded tag가 아닌 PII 없는 구조화 로그에만 기록한다")
    void targetPrivilegeExpansionLogOnly() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.SHADOW,
            Optional.of(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE),
            false,
            Optional.empty()
        ));

        ILoggingEvent event = onlyEvent();
        assertThat(structuredValue(event, "legacyEffect")).isEqualTo("DENY");
        assertThat(structuredValue(event, "targetEffect")).isEqualTo("ALLOW");
        assertThat(structuredValue(event, "targetPrivilegeExpansion")).isEqualTo("YES");
        assertThat(registry.getMeters())
            .flatExtracting(meter -> meter.getId().getTags())
            .extracting(tag -> tag.getKey())
            .doesNotContain("legacyEffect", "targetEffect", "targetPrivilegeExpansion");
    }

    @Test
    @DisplayName("관측 계약과 출력에는 member resource role exception 원문을 수용하지 않는다")
    void noSensitiveInputOrOutput() {
        observer.observe(observation(
            ProjectAuthorizationRolloutMode.SHADOW,
            Optional.of(ProjectAuthorizationClassification.MATCH),
            false,
            Optional.empty()
        ));

        Set<String> forbiddenFragments = Set.of("member", "resource", "role", "exception", "message");
        assertThat(ProjectAuthorizationRolloutObserver.Observation.class.getRecordComponents())
            .extracting(component -> component.getName().toLowerCase())
            .allSatisfy(name -> assertThat(forbiddenFragments)
                .noneSatisfy(fragment -> assertThat(name).contains(fragment)));
        assertThat(registry.getMeters())
            .flatExtracting(meter -> meter.getId().getTags())
            .allSatisfy(tag -> assertThat(tag.getValue().toLowerCase())
                .doesNotContain("member-991", "resource-882", "role-secret", "exception-secret"));
        assertThat(onlyEvent().getFormattedMessage().toLowerCase())
            .doesNotContain("member-991", "resource-882", "role-secret", "exception-secret");
    }

    private ProjectAuthorizationRolloutObserver.Observation observation(
        ProjectAuthorizationRolloutMode mode,
        Optional<ProjectAuthorizationClassification> classification,
        boolean bothFailed,
        Optional<ProjectAuthorizationEvaluationFailureCode> failureCode
    ) {
        return new ProjectAuthorizationRolloutObserver.Observation(
            ProjectPolicyAction.PROJECT_READ,
            mode,
            classification,
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR),
            bothFailed,
            failureCode,
            mode == ProjectAuthorizationRolloutMode.LEGACY
                ? ProjectAuthorizationRolloutObserver.EvaluationEffect.ALLOW
                : ProjectAuthorizationRolloutObserver.EvaluationEffect.DENY,
            mode == ProjectAuthorizationRolloutMode.LEGACY
                ? ProjectAuthorizationRolloutObserver.EvaluationEffect.NOT_EVALUATED
                : ProjectAuthorizationRolloutObserver.EvaluationEffect.ALLOW,
            mode == ProjectAuthorizationRolloutMode.LEGACY
                ? ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.NOT_EVALUATED
                : ProjectAuthorizationRolloutObserver.TargetPrivilegeExpansion.YES,
            Instant.parse("2026-07-14T00:00:00Z"),
            "1.0",
            "project-1.0",
            "1.0.0",
            "ce61cd46827cf7b277d607d7626dfb2817b1f17893a324a3b949ec6582ecf2bb",
            25_000_000L
        );
    }

    private ILoggingEvent onlyEvent() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0);
    }

    private String structuredValue(ILoggingEvent event, String key) {
        List<Object> arguments = List.of(event.getArgumentArray());
        return arguments.stream()
            .filter(StructuredArgument.class::isInstance)
            .map(StructuredArgument.class::cast)
            .map(Object::toString)
            .filter(argument -> argument.startsWith(key + "="))
            .map(argument -> argument.substring(key.length() + 1))
            .findFirst()
            .orElse(null);
    }
}
