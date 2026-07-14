package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.ClassificationScope;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Document;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Entry;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectExpectedDifferenceContractTest {

    private final Document document = new ProjectExpectedDifferenceJsonParser().parseClasspath();

    @TestFactory
    @DisplayName("모든 expected-difference 행은 exact positive witness를 분류한다")
    Stream<DynamicTest> everyRowHasPositiveWitness() {
        return document.entries().stream().map(entry -> DynamicTest.dynamicTest(entry.id(), () -> {
            LoadedProjectExpectedDifferenceMatrix matrix = matrix(entry.predicateAttribute());
            if (entry.classificationScope() == ClassificationScope.SYNTHETIC_ONLY) {
                assertThat(matrix.expectedSyntheticDifferenceId(
                    ProjectExpectedDifferenceSyntheticScenario.LEGACY_NULL_SCHEDULER_CALLER,
                    entry.action(), entry.internalOrigin(),
                    ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied()))
                    .contains(entry.testCaseId());
                return;
            }
            ProjectAuthorizationComparisonRequest request = request(entry);
            assertThat(matrix.expectedDifferenceId(
                request,
                ProjectExpectedDecisionTemplate.resolve(entry.expectedLegacy(), request, facts(entry.predicateAttribute())),
                ProjectExpectedDecisionTemplate.resolve(entry.expectedTarget(), request, facts(entry.predicateAttribute()))
            )).contains(entry.testCaseId());
        }));
    }

    @TestFactory
    @DisplayName("모든 행은 predicate 한 fact가 빠지면 expected로 분류하지 않는다")
    Stream<DynamicTest> everyRowHasOneFactNearMiss() {
        return document.entries().stream().map(entry -> DynamicTest.dynamicTest(entry.id(), () -> {
            LoadedProjectExpectedDifferenceMatrix matrix = matrix("near-miss");
            if (entry.classificationScope() == ClassificationScope.SYNTHETIC_ONLY) {
                assertThat(matrix.expectedSyntheticDifferenceId(
                    ProjectExpectedDifferenceSyntheticScenario.TYPED_SCHEDULER_CALLER,
                    entry.action(), entry.internalOrigin(),
                    ProjectAuthorizationDecision.allowed(), ProjectAuthorizationDecision.denied())).isEmpty();
                return;
            }
            ProjectAuthorizationComparisonRequest request = request(entry);
            ProjectAuthorizationDecision legacy = ProjectExpectedDecisionTemplate.resolve(
                entry.expectedLegacy(), request, facts(entry.predicateAttribute()));
            ProjectAuthorizationDecision target = ProjectExpectedDecisionTemplate.resolve(
                entry.expectedTarget(), request, facts(entry.predicateAttribute()));
            assertThat(matrix.expectedDifferenceId(request, legacy, target)).isEmpty();
            assertThat(new ProjectAuthorizationClassifier(matrix).classify(request, legacy, target))
                .isEqualTo(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE);
        }));
    }

    @Test
    @DisplayName("두 행이 동시에 일치하면 first-match 없이 UNEXPECTED로 분류한다")
    void overlapIsUnexpected() {
        Entry original = document.entries().getFirst();
        List<Entry> entries = new ArrayList<>(document.entries());
        entries.add(copyWithId(original, original.id() + "-overlap"));
        LoadedProjectExpectedDifferenceMatrix matrix = new LoadedProjectExpectedDifferenceMatrix(
            new Document(entries), facts(original.predicateAttribute()));
        ProjectAuthorizationComparisonRequest request = request(original);
        ProjectAuthorizationDecision legacy = ProjectExpectedDecisionTemplate.resolve(
            original.expectedLegacy(), request, facts(original.predicateAttribute()));
        ProjectAuthorizationDecision target = ProjectExpectedDecisionTemplate.resolve(
            original.expectedTarget(), request, facts(original.predicateAttribute()));

        assertThat(matrix.expectedDifferenceId(request, legacy, target)).isEmpty();
        assertThat(new ProjectAuthorizationClassifier(matrix).classify(request, legacy, target))
            .isEqualTo(ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE);
    }

    @Test
    @DisplayName("matrix 분류는 SHADOW와 ENFORCE authoritative 선택을 바꾸지 않는다")
    void matrixDoesNotInfluenceAuthoritativeDecision() {
        Entry entry = document.entries().getFirst();
        ProjectAuthorizationComparisonRequest request = request(entry);
        ProjectAuthorizationDecision legacy = ProjectExpectedDecisionTemplate.resolve(
            entry.expectedLegacy(), request, facts(entry.predicateAttribute()));
        ProjectAuthorizationDecision target = ProjectExpectedDecisionTemplate.resolve(
            entry.expectedTarget(), request, facts(entry.predicateAttribute()));
        ProjectAuthorizationClassifier classifier = new ProjectAuthorizationClassifier(
            matrix(entry.predicateAttribute()));

        assertThat(coordinator(legacy, target, classifier, ProjectAuthorizationRolloutMode.SHADOW)
            .coordinate(request).authoritative()).isEqualTo(legacy);
        assertThat(coordinator(legacy, target, classifier, ProjectAuthorizationRolloutMode.ENFORCE)
            .coordinate(request).authoritative()).isEqualTo(target);
    }

    private LoadedProjectExpectedDifferenceMatrix matrix(String trueAttribute) {
        return new LoadedProjectExpectedDifferenceMatrix(document, facts(trueAttribute));
    }

    private ProjectExpectedDifferenceFactProvider facts(String trueAttribute) {
        return new ProjectExpectedDifferenceFactProvider() {
            @Override
            public boolean booleanValue(String name, ProjectAuthorizationComparisonRequest request) {
                return name.equals(trueAttribute);
            }

            @Override
            public Set<Long> longSet(String name, ProjectAuthorizationComparisonRequest request) {
                return switch (name) {
                    case "resource.gisuId" -> Set.of(1L);
                    case "resource.projectId" -> Set.of(10L);
                    case "subject.memberId" -> Set.of(1L);
                    default -> Set.of(20L);
                };
            }
        };
    }

    private ProjectAuthorizationComparisonRequest request(Entry entry) {
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .project(10L, 1L, 20L, ProjectStatus.PENDING_REVIEW)
            .application(30L, ProjectApplicationStatus.SUBMITTED, 2L)
            .matchingRound(40L, 1L, 20L)
            .creatorMemberId(1L)
            .productOwnerMemberId(2L)
            .build();
        ProjectPolicySubjectSnapshot subject = new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), Instant.parse("2026-07-01T00:00:00Z"),
            List.of(), List.of(), Map.of());
        return new ProjectAuthorizationComparisonRequest(
            subject, entry.action(), resource,
            ProjectAuthorizationEvaluationPoint.internal(entry.internalOrigin()));
    }

    private ProjectAuthorizationRolloutCoordinator coordinator(
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target,
        ProjectAuthorizationClassifier classifier,
        ProjectAuthorizationRolloutMode mode
    ) {
        return new ProjectAuthorizationRolloutCoordinator(
            request -> legacy, request -> target, classifier, action -> mode);
    }

    private Entry copyWithId(Entry entry, String id) {
        return new Entry(
            id, entry.testCaseId(), entry.action(), entry.internalOrigin(), entry.classificationScope(),
            entry.predicateAttribute(), entry.expectedLegacy(), entry.expectedTarget(), entry.reason(), entry.owner());
    }
}
