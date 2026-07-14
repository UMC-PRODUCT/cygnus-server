package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public record ProjectAuthorizationEnforcementTarget(
    String policyVersion,
    String policyFingerprint,
    String artifactSha256
) {
    public ProjectAuthorizationEnforcementTarget {
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(policyFingerprint);
        Objects.requireNonNull(artifactSha256);
    }
}
