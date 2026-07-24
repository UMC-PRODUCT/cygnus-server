package com.umc.product.authorization.application.port.in.policy;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;

public record RegisteredPolicyEvaluationRequest(
    PolicyBundleKey bundleKey,
    String actionId,
    PolicyAttributeSet attributes,
    Instant evaluatedAt
) {

    public RegisteredPolicyEvaluationRequest {
        Objects.requireNonNull(bundleKey);
        Objects.requireNonNull(actionId);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(evaluatedAt);
    }
}
