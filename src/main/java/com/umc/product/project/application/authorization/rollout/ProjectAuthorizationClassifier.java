package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public final class ProjectAuthorizationClassifier {

    private final ProjectExpectedDifferenceMatrix expectedDifferences;

    public ProjectAuthorizationClassifier(ProjectExpectedDifferenceMatrix expectedDifferences) {
        this.expectedDifferences = Objects.requireNonNull(expectedDifferences);
    }

    public ProjectAuthorizationClassification classify(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target
    ) {
        if (target instanceof ProjectAuthorizationEvaluationFailure) {
            return ProjectAuthorizationClassification.TARGET_FAILURE;
        }
        if (legacy instanceof ProjectAuthorizationEvaluationFailure) {
            return ProjectAuthorizationClassification.LEGACY_FAILURE;
        }
        ProjectAuthorizationDecision legacyDecision = (ProjectAuthorizationDecision) legacy;
        ProjectAuthorizationDecision targetDecision = (ProjectAuthorizationDecision) target;
        if (legacyDecision.equals(targetDecision)) {
            return ProjectAuthorizationClassification.MATCH;
        }
        return expectedDifferences.expectedDifferenceId(request, legacyDecision, targetDecision).isPresent()
            ? ProjectAuthorizationClassification.EXPECTED_DIFFERENCE
            : ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE;
    }
}
