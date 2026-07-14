package com.umc.product.project.application.authorization.rollout;

public enum ProjectAuthorizationClassification {
    MATCH,
    EXPECTED_DIFFERENCE,
    UNEXPECTED_DIFFERENCE,
    TARGET_FAILURE,
    LEGACY_FAILURE
}
