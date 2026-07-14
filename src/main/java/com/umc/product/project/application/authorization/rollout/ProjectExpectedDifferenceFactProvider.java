package com.umc.product.project.application.authorization.rollout;

import java.util.Set;

interface ProjectExpectedDifferenceFactProvider {

    boolean booleanValue(String name, ProjectAuthorizationComparisonRequest request);

    Set<Long> longSet(String name, ProjectAuthorizationComparisonRequest request);

    static ProjectExpectedDifferenceFactProvider runtime() {
        return new ProjectExpectedDifferenceFactProvider() {
            @Override
            public boolean booleanValue(String name, ProjectAuthorizationComparisonRequest request) {
                return ProjectExpectedDifferenceFacts.booleanValue(name, request);
            }

            @Override
            public Set<Long> longSet(String name, ProjectAuthorizationComparisonRequest request) {
                return ProjectExpectedDifferenceFacts.longSet(name, request);
            }
        };
    }
}
