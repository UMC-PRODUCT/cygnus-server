package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public final class ConfiguredProjectAuthorizationRolloutModeResolver
    implements ProjectAuthorizationRolloutModeResolver {

    private final ProjectAuthorizationRolloutConfiguration configuration;

    public ConfiguredProjectAuthorizationRolloutModeResolver(
        ProjectAuthorizationRolloutConfiguration configuration
    ) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public ProjectAuthorizationRolloutMode resolve(ProjectPolicyAction action) {
        Objects.requireNonNull(action);
        return configuration.actionOverrides().getOrDefault(action, configuration.defaultMode());
    }
}
