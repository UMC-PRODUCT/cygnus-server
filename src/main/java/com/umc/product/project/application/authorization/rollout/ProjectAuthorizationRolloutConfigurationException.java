package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public final class ProjectAuthorizationRolloutConfigurationException extends RuntimeException {

    private final ProjectAuthorizationRolloutFailureCode code;

    public ProjectAuthorizationRolloutConfigurationException(ProjectAuthorizationRolloutFailureCode code) {
        super("Project authorization rollout configuration failed: " + Objects.requireNonNull(code).name());
        this.code = code;
    }

    public ProjectAuthorizationRolloutFailureCode code() {
        return code;
    }
}
