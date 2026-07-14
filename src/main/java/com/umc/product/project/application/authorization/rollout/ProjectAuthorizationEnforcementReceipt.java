package com.umc.product.project.application.authorization.rollout;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public record ProjectAuthorizationEnforcementReceipt(
    String policyVersion,
    String policyFingerprint,
    String artifactSha256,
    ProjectAuthorizationEnforcementWave wave,
    Set<ProjectPolicyAction> actions,
    String approver,
    Instant approvedAt,
    Instant expiresAt
) {
    public ProjectAuthorizationEnforcementReceipt {
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(policyFingerprint);
        Objects.requireNonNull(artifactSha256);
        Objects.requireNonNull(wave);
        actions = Set.copyOf(actions);
        Objects.requireNonNull(approver);
        Objects.requireNonNull(approvedAt);
        Objects.requireNonNull(expiresAt);
    }
}
