package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectPolicyContextBuilderTest {

    private static final Instant NOW = Instant.parse("2026-03-01T00:00:00Z");

    @Test
    @DisplayName("context builder는 해당 action의 required attribute만 정확히 방출한다")
    void emitsOnlyRequiredAttributesForAction() {
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), NOW, List.of(), List.of(), Map.of());
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .project(100L, 1L, 10L, ProjectStatus.IN_PROGRESS)
            .superAdminAllowDraftRead(false)
            .build();

        PolicyAttributeSet attributes = new ProjectPolicyContextBuilder()
            .build(ProjectPolicyAction.PROJECT_READ, subject, resource);
        Set<String> actual = attributes.entries().stream()
            .map(PolicyAttributeSet.PolicyAttribute::name)
            .collect(Collectors.toUnmodifiableSet());
        Set<String> expected = ProjectPolicyDomainSchema.create()
            .action(ProjectPolicyAction.PROJECT_READ.id()).orElseThrow().requiredAttributes();

        assertThat(actual).isEqualTo(expected);
        assertThat(actual).doesNotContain(ProjectPolicyAttributes.SUBJECT_SYSTEM_ID.name());
    }

    @Test
    @DisplayName("required resource fact가 없으면 evaluation 전 context 조립을 fail closed한다")
    void failsClosedWhenRequiredResourceFactIsMissing() {
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), NOW, List.of(), List.of(), Map.of());

        assertThatThrownBy(() -> new ProjectPolicyContextBuilder().build(
            ProjectPolicyAction.PROJECT_READ,
            subject,
            ProjectPolicyResourceContext.builder().projectTarget(1L, 10L).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("resource.project.id");
    }

    @Test
    @DisplayName("snapshot factory는 request 또는 batch당 evaluatedAt을 한 번만 캡처한다")
    void capturesEvaluatedAtOncePerSnapshot() {
        CountingClock clock = new CountingClock(NOW);
        ProjectPolicySnapshotFactory factory = new ProjectPolicySnapshotFactory(clock);

        ProjectPolicySubjectSnapshot snapshot = factory.member(5L, List.of(), List.of(), Map.of());

        assertThat(snapshot.evaluatedAt()).isEqualTo(NOW);
        assertThat(clock.calls()).isOne();
    }

    private static final class CountingClock extends Clock {
        private final Instant instant;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            calls.incrementAndGet();
            return instant;
        }

        private int calls() {
            return calls.get();
        }
    }
}
