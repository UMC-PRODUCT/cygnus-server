package com.umc.product.project.application.authorization.rollout;

@FunctionalInterface
public interface ProjectAuthorizationEvaluator {

    ProjectAuthorizationEvaluationResult evaluate(ProjectAuthorizationComparisonRequest request);
}
