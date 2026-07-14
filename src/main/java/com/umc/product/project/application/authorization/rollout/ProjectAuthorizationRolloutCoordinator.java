package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;
import java.util.Optional;

public final class ProjectAuthorizationRolloutCoordinator {

    private final ProjectAuthorizationEvaluator legacyEvaluator;
    private final ProjectAuthorizationEvaluator targetEvaluator;
    private final ProjectAuthorizationClassifier classifier;
    private final ProjectAuthorizationRolloutModeResolver modeResolver;

    public ProjectAuthorizationRolloutCoordinator(
        ProjectAuthorizationEvaluator legacyEvaluator,
        ProjectAuthorizationEvaluator targetEvaluator,
        ProjectAuthorizationClassifier classifier,
        ProjectAuthorizationRolloutModeResolver modeResolver
    ) {
        this.legacyEvaluator = Objects.requireNonNull(legacyEvaluator);
        this.targetEvaluator = Objects.requireNonNull(targetEvaluator);
        this.classifier = Objects.requireNonNull(classifier);
        this.modeResolver = Objects.requireNonNull(modeResolver);
    }

    public ProjectAuthorizationCoordinationResult coordinate(ProjectAuthorizationComparisonRequest request) {
        Objects.requireNonNull(request);
        ProjectAuthorizationRolloutMode mode = modeResolver.resolve(request.action());
        ProjectAuthorizationEvaluationResult legacy = evaluate(legacyEvaluator, request);
        if (mode == ProjectAuthorizationRolloutMode.LEGACY) {
            return new ProjectAuthorizationCoordinationResult(
                mode, legacy, Optional.empty(), Optional.empty(), legacy);
        }
        ProjectAuthorizationEvaluationResult target = evaluate(targetEvaluator, request);
        ProjectAuthorizationClassification classification = classify(request, legacy, target);
        ProjectAuthorizationEvaluationResult authoritative = switch (mode) {
            case LEGACY -> throw new IllegalStateException("LEGACY mode는 위에서 처리됩니다.");
            case SHADOW -> legacy;
            case ENFORCE -> target;
        };
        return new ProjectAuthorizationCoordinationResult(
            mode, legacy, Optional.of(target), Optional.of(classification), authoritative);
    }

    private ProjectAuthorizationClassification classify(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target
    ) {
        try {
            return classifier.classify(request, legacy, target);
        } catch (RuntimeException exception) {
            ProjectAuthorizationAuxiliaryFailureReporter.warn(
                ProjectAuthorizationAuxiliaryFailureCode.CLASSIFICATION_FAILED,
                request.action()
            );
            return ProjectAuthorizationClassification.UNEXPECTED_DIFFERENCE;
        }
    }

    private ProjectAuthorizationEvaluationResult evaluate(
        ProjectAuthorizationEvaluator evaluator,
        ProjectAuthorizationComparisonRequest request
    ) {
        try {
            return Objects.requireNonNull(evaluator.evaluate(request));
        } catch (RuntimeException exception) {
            return new ProjectAuthorizationEvaluationFailure(
                ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED,
                request.evaluatedAt()
            );
        }
    }
}
