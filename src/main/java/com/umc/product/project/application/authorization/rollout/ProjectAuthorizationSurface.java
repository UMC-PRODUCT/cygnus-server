package com.umc.product.project.application.authorization.rollout;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyGate;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceCatalog;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceDescriptor;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceIdentity;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceType;

public enum ProjectAuthorizationSurface {
    REST_PROJECT_CREATE("rest:POST /api/v1/projects"),
    REST_PROJECT_UPDATE("rest:PATCH /api/v1/projects/{projectId}"),
    REST_PROJECT_SUBMIT("rest:POST /api/v1/projects/{projectId}/submit"),
    REST_PROJECT_TRANSFER_OWNERSHIP("rest:POST /api/v1/projects/{projectId}/transfer-ownership"),
    REST_PROJECT_MEMBER_ADD("rest:POST /api/v1/projects/{projectId}/members"),
    REST_PROJECT_PUBLISH("rest:POST /api/v1/projects/{projectId}/publish"),
    REST_PROJECT_QUOTA_UPDATE("rest:PUT /api/v1/projects/{projectId}/part-quotas"),
    REST_PROJECT_DELETE("rest:DELETE /api/v1/projects/{projectId}"),
    REST_PROJECT_ABORT("rest:POST /api/v1/projects/{projectId}/abort"),
    REST_PROJECT_MEMBER_REMOVE("rest:DELETE /api/v1/projects/{projectId}/members/{memberId}"),
    REST_PROJECT_MEMBER_STATUS_UPDATE("rest:PATCH /api/v1/projects/{projectId}/members/{memberId}/status"),
    REST_PROJECT_LIST_PUBLIC("rest:GET /api/v1/projects"),
    REST_PROJECT_READ("rest:GET /api/v1/projects/{projectId}"),
    REST_PROJECT_MEMBER_LIST("rest:GET /api/v1/projects/{projectId}/members"),
    REST_PROJECT_MEMBER_BATCH("rest:GET /api/v1/projects/members"),
    REST_PROJECT_LIST_MANAGED("rest:GET /api/v1/projects/me/managed"),
    REST_PROJECT_LIST_OWN_DRAFTS("rest:GET /api/v1/projects/me/draft"),
    REST_APPLICATION_CREATE("rest:POST /api/v1/projects/{projectId}/applications"),
    REST_APPLICATION_UPDATE("rest:PUT /api/v1/projects/{projectId}/applications/{applicationId}"),
    REST_APPLICATION_SUBMIT("rest:POST /api/v1/projects/{projectId}/applications/{applicationId}/submit"),
    REST_APPLICATION_DECIDE("rest:PATCH /api/v1/projects/{projectId}/applications/{applicationId}/decision"),
    REST_APPLICATION_CANCEL("rest:DELETE /api/v1/projects/{projectId}/applications/{applicationId}"),
    REST_APPLICATION_LIST_SELF("rest:GET /api/v1/projects/me/applications"),
    REST_APPLICATION_LIST_BATCH("rest:GET /api/v1/projects/applications"),
    REST_APPLICATION_LIST_PROJECT("rest:GET /api/v1/projects/{projectId}/applications"),
    REST_APPLICATION_READ("rest:GET /api/v1/projects/{projectId}/applications/{applicationId}"),
    REST_FORM_UPDATE("rest:PUT /api/v1/projects/{projectId}/application-form"),
    REST_FORM_READ("rest:GET /api/v1/projects/{projectId}/application-form"),
    REST_MATCHING_LIST("rest:GET /api/v1/project/matching-rounds"),
    REST_MATCHING_CREATE("rest:POST /api/v1/project/matching-rounds"),
    REST_MATCHING_UPDATE("rest:PATCH /api/v1/project/matching-rounds/{matchingRoundId}"),
    REST_MATCHING_DELETE("rest:DELETE /api/v1/project/matching-rounds/{matchingRoundId}"),
    REST_MATCHING_HUMAN_AUTO_DECIDE(
        "rest:POST /api/v1/project/matching-rounds/{matchingRoundId}/auto-decide"),
    REST_STATISTICS_PROJECT("rest:GET /api/v1/projects/{projectId}/statistics"),
    REST_STATISTICS_CHAPTER("rest:GET /api/v1/projects/statistics"),
    REST_STATISTICS_PUBLIC_MATCHING("rest:GET /api/v1/projects/statistics/matchings"),
    REST_CAPABILITY_LIST("rest:GET /api/v1/projects/permissions"),
    GRAPHQL_PROJECT("graphql:Query.project"),
    GRAPHQL_PROJECTS("graphql:Query.projects"),
    GRAPHQL_PROJECT_MEMBERS("graphql:Project.members"),
    GRAPHQL_APPLICATION_FORM("graphql:Project.applicationForm"),
    GRAPHQL_PROJECT_MEMBER_APPLICATION("graphql:ProjectMember.application"),
    GRAPHQL_PROJECT_PRODUCT_OWNER("graphql:Project.productOwner"),
    GRAPHQL_PROJECT_CO_PRODUCT_OWNERS("graphql:Project.coProductOwners"),
    GRAPHQL_PROJECT_MEMBER_MEMBER("graphql:ProjectMember.member"),
    SCHEDULER_MATCHING_ROUND_DEADLINE("scheduler:matching-round-deadline");

    private final String id;

    ProjectAuthorizationSurface(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public ProjectPolicySurfaceIdentity identity() {
        return descriptor().identity();
    }

    public ProjectPolicyAction action() {
        return descriptor().action();
    }

    public ProjectPolicyGate gate() {
        return descriptor().gate();
    }

    public ProjectPolicySurfaceType type() {
        return descriptor().type();
    }

    private ProjectPolicySurfaceDescriptor descriptor() {
        return ProjectPolicySurfaceCatalog.surfaces().stream()
            .filter(surface -> surface.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("catalog에 없는 rollout surface입니다: " + name()));
    }
}
