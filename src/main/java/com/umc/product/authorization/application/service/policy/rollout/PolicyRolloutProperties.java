package com.umc.product.authorization.application.service.policy.rollout;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

@ConfigurationProperties(prefix = "app.authorization.rollout")
public record PolicyRolloutProperties(
    PolicyRolloutMode defaultMode,
    List<ActionOverride> overrides
) {

    public PolicyRolloutProperties {
        if (defaultMode == null) {
            defaultMode = PolicyRolloutMode.SHADOW;
        }
        overrides = overrides == null ? List.of() : List.copyOf(overrides);
    }

    public record ActionOverride(
        String namespace,
        String action,
        PolicyRolloutMode mode
    ) {
    }
}
