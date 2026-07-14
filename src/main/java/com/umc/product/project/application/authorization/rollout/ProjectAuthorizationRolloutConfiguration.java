package com.umc.product.project.application.authorization.rollout;

import java.util.Map;
import java.util.Objects;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public record ProjectAuthorizationRolloutConfiguration(
    ProjectAuthorizationRolloutMode defaultMode,
    Map<ProjectPolicyAction, ProjectAuthorizationRolloutMode> actionOverrides
) {
    public ProjectAuthorizationRolloutConfiguration {
        Objects.requireNonNull(defaultMode);
        actionOverrides = Map.copyOf(actionOverrides);
    }
}
