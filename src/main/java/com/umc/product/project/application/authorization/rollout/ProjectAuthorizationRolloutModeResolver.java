package com.umc.product.project.application.authorization.rollout;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

@FunctionalInterface
public interface ProjectAuthorizationRolloutModeResolver {

    ProjectAuthorizationRolloutMode resolve(ProjectPolicyAction action);
}
