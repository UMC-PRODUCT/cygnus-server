package com.umc.product.authorization.domain.policy.rollout;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record PolicyEnforcementReceipt(
    String policyVersion,
    String policyFingerprint,
    String artifactSha256,
    Set<String> actions,
    String approver,
    Instant approvedAt,
    Instant expiresAt
) {

    public PolicyEnforcementReceipt {
        requireText(policyVersion, "policyVersion");
        requireText(policyFingerprint, "policyFingerprint");
        requireText(artifactSha256, "artifactSha256");
        actions = Set.copyOf(actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("Policy enforcement receipt action은 비어 있을 수 없습니다.");
        }
        actions.forEach(action -> requireText(action, "action"));
        requireText(approver, "approver");
        Objects.requireNonNull(approvedAt);
        Objects.requireNonNull(expiresAt);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "Policy enforcement receipt " + field + "는 비어 있을 수 없습니다.");
        }
    }
}
