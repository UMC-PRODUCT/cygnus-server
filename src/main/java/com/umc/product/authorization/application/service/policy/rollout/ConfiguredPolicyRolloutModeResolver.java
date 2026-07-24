package com.umc.product.authorization.application.service.policy.rollout;

import java.util.Objects;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

public final class ConfiguredPolicyRolloutModeResolver implements PolicyRolloutModeResolver {

    private final PolicyRolloutConfiguration configuration;

    public ConfiguredPolicyRolloutModeResolver(PolicyRolloutConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public PolicyRolloutMode resolve(PolicyRolloutKey key) {
        Objects.requireNonNull(key);
        return configuration.overrides().getOrDefault(key, configuration.defaultMode());
    }
}
