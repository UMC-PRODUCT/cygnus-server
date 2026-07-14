package com.umc.product.project.application.authorization.rollout;

public sealed interface ProjectAuthorizationEvaluationResult
    permits ProjectAuthorizationDecision, ProjectAuthorizationEvaluationFailure {

    boolean allows();
}
