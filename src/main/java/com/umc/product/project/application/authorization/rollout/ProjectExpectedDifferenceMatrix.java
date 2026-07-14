package com.umc.product.project.application.authorization.rollout;

import java.util.Optional;

@FunctionalInterface
public interface ProjectExpectedDifferenceMatrix {

    Optional<ProjectExpectedDifferenceId> expectedDifferenceId(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    );
}
