package com.umc.product.project.application.authorization.rollout;

@FunctionalInterface
public interface ProjectExpectedDifferenceContextPredicate {

    boolean matches(ProjectAuthorizationComparisonRequest request);
}
