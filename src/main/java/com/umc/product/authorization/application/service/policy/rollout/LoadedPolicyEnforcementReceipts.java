package com.umc.product.authorization.application.service.policy.rollout;

import java.util.Objects;

import com.umc.product.authorization.domain.policy.rollout.PolicyEnforcementReceiptDocument;

public record LoadedPolicyEnforcementReceipts(
    PolicyEnforcementReceiptDocument document,
    String artifactSha256
) {

    public LoadedPolicyEnforcementReceipts {
        Objects.requireNonNull(document);
        Objects.requireNonNull(artifactSha256);
    }
}
