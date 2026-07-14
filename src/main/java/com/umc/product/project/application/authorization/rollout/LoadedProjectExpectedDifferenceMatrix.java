package com.umc.product.project.application.authorization.rollout;

import java.util.List;
import java.util.Optional;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.ClassificationScope;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Document;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Entry;

final class LoadedProjectExpectedDifferenceMatrix implements ProjectExpectedDifferenceMatrix {

    private final List<Entry> entries;
    private final ProjectExpectedDifferenceFactProvider facts;

    LoadedProjectExpectedDifferenceMatrix(Document document) {
        this(document, ProjectExpectedDifferenceFactProvider.runtime());
    }

    LoadedProjectExpectedDifferenceMatrix(
        Document document,
        ProjectExpectedDifferenceFactProvider facts
    ) {
        entries = document.entries();
        this.facts = facts;
        entries.forEach(this::validate);
    }

    @Override
    public Optional<ProjectExpectedDifferenceId> expectedDifferenceId(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    ) {
        List<Entry> matches = entries.stream()
            .filter(entry -> entry.classificationScope() == ClassificationScope.RUNTIME)
            .filter(entry -> entry.action() == request.action())
            .filter(entry -> evaluationPointMatches(entry, request.evaluationPoint()))
            .filter(entry -> facts.booleanValue(entry.predicateAttribute(), request))
            .filter(entry -> decisionsMatch(entry, request, legacy, target))
            .toList();
        return matches.size() == 1
            ? Optional.of(matches.getFirst().testCaseId())
            : Optional.empty();
    }

    int rowCount() {
        return entries.size();
    }

    long rowCount(ProjectExpectedDifferenceId id) {
        return entries.stream().filter(entry -> entry.testCaseId() == id).count();
    }

    boolean hasSyntheticWitness(ProjectExpectedDifferenceId id, ProjectPolicyAction action) {
        return entries.stream().anyMatch(entry -> entry.testCaseId() == id
            && entry.action() == action
            && entry.classificationScope() == ClassificationScope.SYNTHETIC_ONLY);
    }

    Optional<ProjectExpectedDifferenceId> expectedSyntheticDifferenceId(
        ProjectExpectedDifferenceSyntheticScenario scenario,
        ProjectPolicyAction action,
        ProjectAuthorizationInternalOrigin origin,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    ) {
        List<Entry> matches = entries.stream()
            .filter(entry -> entry.classificationScope() == ClassificationScope.SYNTHETIC_ONLY)
            .filter(entry -> entry.action() == action && entry.internalOrigin() == origin)
            .filter(entry -> syntheticPredicateMatches(entry, scenario))
            .filter(entry -> entry.expectedLegacy().effect() == legacy.effect())
            .filter(entry -> entry.expectedTarget().effect() == target.effect())
            .toList();
        return matches.size() == 1
            ? Optional.of(matches.getFirst().testCaseId())
            : Optional.empty();
    }

    List<Entry> entries() {
        return entries;
    }

    private boolean decisionsMatch(
        Entry entry,
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    ) {
        return ProjectExpectedDecisionTemplate.resolve(entry.expectedLegacy(), request, facts).equals(legacy)
            && ProjectExpectedDecisionTemplate.resolve(entry.expectedTarget(), request, facts).equals(target);
    }

    private boolean evaluationPointMatches(Entry entry, ProjectAuthorizationEvaluationPoint point) {
        return point instanceof ProjectAuthorizationEvaluationPoint.Internal internal
            && internal.origin() == entry.internalOrigin();
    }

    private void validate(Entry entry) {
        if (!ProjectExpectedDifferenceFacts.supportsBoolean(entry.predicateAttribute())) {
            throw new IllegalStateException("EXPECTED_DIFFERENCE_ATTRIBUTE_UNSUPPORTED");
        }
        ProjectExpectedDecisionTemplate.validate(entry.expectedLegacy());
        ProjectExpectedDecisionTemplate.validate(entry.expectedTarget());
    }

    private boolean syntheticPredicateMatches(
        Entry entry,
        ProjectExpectedDifferenceSyntheticScenario scenario
    ) {
        return "synthetic.nullableSchedulerCaller".equals(entry.predicateAttribute())
            && scenario == ProjectExpectedDifferenceSyntheticScenario.LEGACY_NULL_SCHEDULER_CALLER;
    }
}
