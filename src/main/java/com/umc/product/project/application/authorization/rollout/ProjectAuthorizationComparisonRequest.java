package com.umc.product.project.application.authorization.rollout;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;

public record ProjectAuthorizationComparisonRequest(
    ProjectPolicySubjectSnapshot snapshot,
    ProjectPolicyAction action,
    ProjectAuthorizationResourceSnapshot resourceSnapshot,
    ProjectAuthorizationEvaluationPoint evaluationPoint
) {
    public ProjectAuthorizationComparisonRequest {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(action);
        Objects.requireNonNull(resourceSnapshot);
        Objects.requireNonNull(evaluationPoint);
        validateSurfaceAction(action, evaluationPoint);
    }

    public ProjectAuthorizationComparisonRequest(
        ProjectPolicySubjectSnapshot snapshot,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        this(snapshot, action, ProjectAuthorizationResourceSnapshot.of(resource), evaluationPoint);
    }

    public ProjectPolicyResourceContext resource() {
        return resourceSnapshot.policyContext();
    }

    public Instant evaluatedAt() {
        return snapshot.evaluatedAt();
    }

    private static void validateSurfaceAction(
        ProjectPolicyAction action,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        if (!(evaluationPoint instanceof ProjectAuthorizationEvaluationPoint.Surface surface)) {
            return;
        }
        ProjectPolicyAction catalogAction = surface.surface().action();
        if (catalogAction != action) {
            throw new IllegalArgumentException(
                "Project 정책 호출면 action이 일치하지 않습니다: " + surface.surface().id());
        }
    }
}
