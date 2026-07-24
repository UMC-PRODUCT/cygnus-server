package com.umc.product.authorization.application.service.policy.rollout;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

public record PolicyRolloutConfiguration(
    PolicyRolloutMode defaultMode,
    Map<PolicyRolloutKey, PolicyRolloutMode> overrides
) {

    public PolicyRolloutConfiguration {
        Objects.requireNonNull(defaultMode);
        overrides = java.util.Collections.unmodifiableMap(new TreeMap<>(overrides));
    }
}
