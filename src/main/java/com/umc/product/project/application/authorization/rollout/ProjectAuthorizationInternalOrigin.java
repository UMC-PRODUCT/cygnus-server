package com.umc.product.project.application.authorization.rollout;

public enum ProjectAuthorizationInternalOrigin {
    RESOURCE_PERMISSION_EVALUATOR,
    PROJECT_SCOPE_RESOLVER,
    APPLICATION_SCOPE_RESOLVER,
    FORM_ACCESS_POLICY,
    STATISTICS_ACCESS_POLICY,
    CAPABILITY_QUERY,
    PARENT_TRANSITIVE,
    MATCHING_COMMAND,
    MATCHING_SCHEDULER
}
