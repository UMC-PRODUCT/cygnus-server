package com.umc.product.project.application.authorization;

import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_CANCEL;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_PROJECT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_SELF;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_SUBMIT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.CAPABILITY_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.FORM_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.FORM_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_DELETE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_ABORT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_DELETE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_MANAGED;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_PUBLIC;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_ADD;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_BATCH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_REMOVE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_PUBLISH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_QUOTA_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_SUBMIT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_CHAPTER;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_PROJECT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.ACTOR;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.DIRECT;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.PUBLIC;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.RESOURCE;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.SYSTEM;
import static com.umc.product.project.application.authorization.ProjectPolicyGate.TRANSITIVE;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.APPLICATION_RESOURCE;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.APPLICATION_SCOPE;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.FORM;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.MATCHING_ROUND;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.PROJECT_RESOURCE;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.PROJECT_SCOPE;
import static com.umc.product.project.application.authorization.ProjectPolicyModule.STATISTICS;
import static com.umc.product.project.application.authorization.ProjectPolicySurfaceType.GRAPHQL;
import static com.umc.product.project.application.authorization.ProjectPolicySurfaceType.REST;
import static com.umc.product.project.application.authorization.ProjectPolicySurfaceType.SCHEDULER;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProjectPolicySurfaceCatalog {

    private static final String WEB = "com.umc.product.project.adapter.in.web.";
    private static final String GRAPHQL_CONTROLLER =
        "com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#";
    private static final String SCHEDULER_HANDLER =
        "com.umc.product.project.adapter.in.scheduler.MatchingRoundDeadlineHandler#handle";

    private static final List<ProjectPolicySurfaceDescriptor> SURFACES = buildSurfaces();

    private ProjectPolicySurfaceCatalog() {
    }

    public static List<ProjectPolicySurfaceDescriptor> surfaces() {
        return SURFACES;
    }

    public static Set<ProjectPolicySurfaceIdentity> identities() {
        return SURFACES.stream()
            .map(ProjectPolicySurfaceDescriptor::identity)
            .collect(Collectors.toUnmodifiableSet());
    }

    public static void assertExactIdentities(Set<ProjectPolicySurfaceIdentity> discovered) {
        Set<ProjectPolicySurfaceIdentity> missing = new HashSet<>(identities());
        missing.removeAll(discovered);
        Set<ProjectPolicySurfaceIdentity> unexpected = new HashSet<>(discovered);
        unexpected.removeAll(identities());
        if (!missing.isEmpty() || !unexpected.isEmpty()) {
            throw new IllegalStateException(
                "Project 정책 호출면이 일치하지 않습니다. missing=" + sorted(missing)
                    + ", unexpected=" + sorted(unexpected)
            );
        }
    }

    private static List<ProjectPolicySurfaceDescriptor> buildSurfaces() {
        List<ProjectPolicySurfaceDescriptor> surfaces = new ArrayList<>();
        addProjectCommand(surfaces);
        addProjectQuery(surfaces);
        addApplication(surfaces);
        addFormMatchingStatisticsCapability(surfaces);
        addGraphQlAndScheduler(surfaces);
        validateUniqueIds(surfaces);
        return surfaces.stream()
            .sorted(Comparator.comparing(ProjectPolicySurfaceDescriptor::id))
            .toList();
    }

    private static void addProjectCommand(List<ProjectPolicySurfaceDescriptor> surfaces) {
        String handler = WEB + "ProjectCommandController#";
        surfaces.add(surface("rest:POST /api/v1/projects", handler + "createDraft", REST, PROJECT_CREATE, PROJECT_RESOURCE, DIRECT));
        surfaces.add(surface("rest:PATCH /api/v1/projects/{projectId}", handler + "update", REST, PROJECT_UPDATE, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/submit", handler + "submit", REST, PROJECT_SUBMIT, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/transfer-ownership", handler + "transferOwnership", REST, PROJECT_TRANSFER_OWNERSHIP, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/members", handler + "addMember", REST, PROJECT_MEMBER_ADD, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/publish", handler + "publish", REST, PROJECT_PUBLISH, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:PUT /api/v1/projects/{projectId}/part-quotas", handler + "updatePartQuotas", REST, PROJECT_QUOTA_UPDATE, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:DELETE /api/v1/projects/{projectId}", handler + "delete", REST, PROJECT_DELETE, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/abort", handler + "abort", REST, PROJECT_ABORT, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:DELETE /api/v1/projects/{projectId}/members/{memberId}", handler + "removeMember", REST, PROJECT_MEMBER_REMOVE, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:PATCH /api/v1/projects/{projectId}/members/{memberId}/status", handler + "changeMemberStatus", REST, PROJECT_MEMBER_STATUS_UPDATE, PROJECT_RESOURCE, RESOURCE));
    }

    private static void addProjectQuery(List<ProjectPolicySurfaceDescriptor> surfaces) {
        String handler = WEB + "ProjectQueryController#";
        surfaces.add(surface("rest:GET /api/v1/projects", handler + "searchProjects", REST, PROJECT_LIST_PUBLIC, PROJECT_SCOPE, ACTOR));
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}", handler + "getDetail", REST, PROJECT_READ, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}/members", handler + "getMembers", REST, PROJECT_MEMBER_LIST, PROJECT_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:GET /api/v1/projects/members", handler + "getBatchMembers", REST, PROJECT_MEMBER_BATCH, PROJECT_RESOURCE, ACTOR));
        surfaces.add(surface("rest:GET /api/v1/projects/me/managed", handler + "searchManaged", REST, PROJECT_LIST_MANAGED, PROJECT_SCOPE, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/me/draft", handler + "getMyDraft", REST, PROJECT_LIST_OWN_DRAFTS, PROJECT_SCOPE, DIRECT));
    }

    private static void addApplication(List<ProjectPolicySurfaceDescriptor> surfaces) {
        String command = WEB + "ProjectApplicationController#";
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/applications", command + "createDraft", REST, APPLICATION_CREATE, APPLICATION_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:PUT /api/v1/projects/{projectId}/applications/{applicationId}", command + "updateDraft", REST, APPLICATION_UPDATE, APPLICATION_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:POST /api/v1/projects/{projectId}/applications/{applicationId}/submit", command + "submit", REST, APPLICATION_SUBMIT, APPLICATION_RESOURCE, RESOURCE));
        surfaces.add(surface("rest:PATCH /api/v1/projects/{projectId}/applications/{applicationId}/decision", command + "decide", REST, APPLICATION_DECIDE, APPLICATION_RESOURCE, DIRECT));
        surfaces.add(surface("rest:DELETE /api/v1/projects/{projectId}/applications/{applicationId}", command + "cancel", REST, APPLICATION_CANCEL, APPLICATION_RESOURCE, RESOURCE));

        String query = WEB + "ProjectApplicationQueryController#";
        surfaces.add(surface("rest:GET /api/v1/projects/me/applications", query + "getMyApplications", REST, APPLICATION_LIST_SELF, APPLICATION_SCOPE, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/applications", query + "getProjectApplicantsBatch", REST, APPLICATION_LIST_PROJECT_BATCH, APPLICATION_SCOPE, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}/applications", query + "getProjectApplicants", REST, APPLICATION_LIST_PROJECT, APPLICATION_SCOPE, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}/applications/{applicationId}", query + "getApplicationDetail", REST, APPLICATION_READ, APPLICATION_RESOURCE, RESOURCE));
    }

    private static void addFormMatchingStatisticsCapability(List<ProjectPolicySurfaceDescriptor> surfaces) {
        String form = WEB + "ProjectApplicationFormController#";
        surfaces.add(surface("rest:PUT /api/v1/projects/{projectId}/application-form", form + "upsert", REST, FORM_UPDATE, FORM, RESOURCE));
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}/application-form", form + "get", REST, FORM_READ, FORM, RESOURCE));

        String matching = WEB + "ProjectMatchingRoundController#";
        surfaces.add(surface("rest:GET /api/v1/project/matching-rounds", matching + "list", REST, MATCHING_LIST, MATCHING_ROUND, DIRECT));
        surfaces.add(surface("rest:POST /api/v1/project/matching-rounds", matching + "create", REST, MATCHING_CREATE, MATCHING_ROUND, DIRECT));
        surfaces.add(surface("rest:PATCH /api/v1/project/matching-rounds/{matchingRoundId}", matching + "update", REST, MATCHING_UPDATE, MATCHING_ROUND, DIRECT));
        surfaces.add(surface("rest:DELETE /api/v1/project/matching-rounds/{matchingRoundId}", matching + "delete", REST, MATCHING_DELETE, MATCHING_ROUND, DIRECT));
        surfaces.add(surface("rest:POST /api/v1/project/matching-rounds/{matchingRoundId}/auto-decide", matching + "autoDecide", REST, MATCHING_HUMAN_AUTO_DECIDE, MATCHING_ROUND, DIRECT));

        String statistics = WEB + "ProjectStatisticsQueryController#";
        surfaces.add(surface("rest:GET /api/v1/projects/{projectId}/statistics", statistics + "getProjectStatistics", REST, STATISTICS_PROJECT, STATISTICS, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/statistics", statistics + "getStatistics", REST, STATISTICS_CHAPTER, STATISTICS, DIRECT));
        surfaces.add(surface("rest:GET /api/v1/projects/statistics/matchings", statistics + "getPublicMatchingStatistics", REST, STATISTICS_PUBLIC_MATCHING, STATISTICS, PUBLIC));

        surfaces.add(surface("rest:GET /api/v1/projects/permissions", WEB + "ProjectPermissionController#getPermissions", REST, CAPABILITY_LIST, PROJECT_RESOURCE, DIRECT));
    }

    private static void addGraphQlAndScheduler(List<ProjectPolicySurfaceDescriptor> surfaces) {
        surfaces.add(surface("graphql:Query.project", GRAPHQL_CONTROLLER + "project", GRAPHQL, PROJECT_READ, PROJECT_RESOURCE, DIRECT));
        surfaces.add(surface("graphql:Query.projects", GRAPHQL_CONTROLLER + "projects", GRAPHQL, PROJECT_LIST_PUBLIC, PROJECT_SCOPE, DIRECT));
        surfaces.add(surface("graphql:Project.members", GRAPHQL_CONTROLLER + "membersByProject", GRAPHQL, PROJECT_MEMBER_LIST, PROJECT_RESOURCE, DIRECT));
        surfaces.add(surface("graphql:Project.applicationForm", GRAPHQL_CONTROLLER + "applicationFormByProject", GRAPHQL, FORM_READ, FORM, DIRECT));
        surfaces.add(surface("graphql:ProjectMember.application", GRAPHQL_CONTROLLER + "applicationByProjectMember", GRAPHQL, APPLICATION_READ, APPLICATION_RESOURCE, DIRECT));
        surfaces.add(surface("graphql:Project.productOwner", GRAPHQL_CONTROLLER + "productOwnerByProject", GRAPHQL, PROJECT_READ, PROJECT_RESOURCE, TRANSITIVE));
        surfaces.add(surface("graphql:Project.coProductOwners", GRAPHQL_CONTROLLER + "coProductOwnersByProject", GRAPHQL, PROJECT_READ, PROJECT_RESOURCE, TRANSITIVE));
        surfaces.add(surface("graphql:ProjectMember.member", GRAPHQL_CONTROLLER + "memberByProjectMember", GRAPHQL, PROJECT_READ, PROJECT_RESOURCE, TRANSITIVE));
        surfaces.add(surface("scheduler:matching-round-deadline", SCHEDULER_HANDLER, SCHEDULER, MATCHING_SYSTEM_AUTO_DECIDE, MATCHING_ROUND, SYSTEM));
    }

    private static ProjectPolicySurfaceDescriptor surface(
        String id,
        String handler,
        ProjectPolicySurfaceType type,
        ProjectPolicyAction action,
        ProjectPolicyModule module,
        ProjectPolicyGate gate
    ) {
        return new ProjectPolicySurfaceDescriptor(
            new ProjectPolicySurfaceIdentity(id, handler),
            type,
            action,
            module,
            gate
        );
    }

    private static void validateUniqueIds(List<ProjectPolicySurfaceDescriptor> surfaces) {
        Set<String> ids = new HashSet<>();
        for (ProjectPolicySurfaceDescriptor surface : surfaces) {
            if (!ids.add(surface.id())) {
                throw new IllegalStateException("중복된 Project 정책 호출면 ID입니다: " + surface.id());
            }
        }
    }

    private static List<String> sorted(Set<ProjectPolicySurfaceIdentity> identities) {
        return identities.stream()
            .map(identity -> identity.id() + " -> " + identity.handler())
            .sorted()
            .toList();
    }
}
