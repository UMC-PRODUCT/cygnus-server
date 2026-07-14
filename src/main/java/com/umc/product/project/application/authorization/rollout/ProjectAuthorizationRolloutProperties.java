package com.umc.product.project.application.authorization.rollout;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.project.authorization-rollout")
public record ProjectAuthorizationRolloutProperties(
    ProjectAuthorizationRolloutMode defaultMode,
    List<ActionOverride> actionOverrides
) {

    public ProjectAuthorizationRolloutProperties {
        if (defaultMode == null) {
            defaultMode = ProjectAuthorizationRolloutMode.SHADOW;
        }
        actionOverrides = actionOverrides == null ? List.of() : List.copyOf(actionOverrides);
    }

    public record ActionOverride(
        String action,
        ProjectAuthorizationRolloutMode mode
    ) {
    }
}
