package com.umc.product.authorization.application.port.in.policy;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;

public record PolicyEvaluationRequest(
        CompiledPolicyBundle bundle, String actionId, PolicyAttributeSet attributes, Instant evaluatedAt) {

    public PolicyEvaluationRequest {
        Objects.requireNonNull(bundle);
        Objects.requireNonNull(actionId);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(evaluatedAt);
    }
}
