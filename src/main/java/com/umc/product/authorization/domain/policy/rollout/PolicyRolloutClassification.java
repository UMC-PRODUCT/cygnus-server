package com.umc.product.authorization.domain.policy.rollout;

public enum PolicyRolloutClassification {
    MATCH,
    EXPECTED_DIFFERENCE,
    UNEXPECTED_DIFFERENCE,
    TARGET_FAILURE,
    LEGACY_FAILURE
}
