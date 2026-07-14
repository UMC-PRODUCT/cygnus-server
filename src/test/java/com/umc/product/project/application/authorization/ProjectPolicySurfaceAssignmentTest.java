package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectPolicySurfaceAssignmentTest {

    @Test
    @DisplayName("46개 호출면은 승인된 semantic action과 module에 정확히 연결된다")
    void surfacesMapToExactActionsAndModules() {
        Map<String, ProjectPolicySurfaceDescriptor> catalog = ProjectPolicySurfaceCatalog.surfaces().stream()
            .collect(Collectors.toUnmodifiableMap(ProjectPolicySurfaceDescriptor::id, Function.identity()));
        Map<String, ProjectPolicyAction> expectedActions = expectedActions();

        assertThat(catalog).hasSize(46);
        assertThat(catalog.keySet()).containsExactlyInAnyOrderElementsOf(expectedActions.keySet());
        expectedActions.forEach((surfaceId, action) -> {
            ProjectPolicySurfaceDescriptor descriptor = catalog.get(surfaceId);
            assertThat(descriptor.action()).as(surfaceId).isEqualTo(action);
            assertThat(descriptor.module()).as(surfaceId).isEqualTo(expectedModule(action));
        });
        assertThat(ProjectPolicyModule.values())
            .extracting(ProjectPolicyModule::id)
            .containsExactlyInAnyOrder(
                "project-resource",
                "project-scope",
                "application-resource",
                "application-scope",
                "form",
                "statistics",
                "matching-round"
            );
    }

    @Test
    @DisplayName("GraphQL parent 보호와 scheduler system gate를 명시적으로 고정한다")
    void graphqlAndSchedulerGatesAreExplicit() {
        Map<String, ProjectPolicyGate> gates = ProjectPolicySurfaceCatalog.surfaces().stream()
            .filter(surface -> surface.type() != ProjectPolicySurfaceType.REST)
            .collect(Collectors.toUnmodifiableMap(ProjectPolicySurfaceDescriptor::id, ProjectPolicySurfaceDescriptor::gate));

        assertThat(gates).containsExactlyInAnyOrderEntriesOf(Map.of(
            "graphql:Query.project", ProjectPolicyGate.DIRECT,
            "graphql:Query.projects", ProjectPolicyGate.DIRECT,
            "graphql:Project.members", ProjectPolicyGate.DIRECT,
            "graphql:Project.applicationForm", ProjectPolicyGate.DIRECT,
            "graphql:ProjectMember.application", ProjectPolicyGate.DIRECT,
            "graphql:Project.productOwner", ProjectPolicyGate.TRANSITIVE,
            "graphql:Project.coProductOwners", ProjectPolicyGate.TRANSITIVE,
            "graphql:ProjectMember.member", ProjectPolicyGate.TRANSITIVE,
            "scheduler:matching-round-deadline", ProjectPolicyGate.SYSTEM
        ));
    }

    private Map<String, ProjectPolicyAction> expectedActions() {
        return Map.ofEntries(
            Map.entry("rest:POST /api/v1/projects", ProjectPolicyAction.PROJECT_CREATE),
            Map.entry("rest:PATCH /api/v1/projects/{projectId}", ProjectPolicyAction.PROJECT_UPDATE),
            Map.entry("rest:POST /api/v1/projects/{projectId}/submit", ProjectPolicyAction.PROJECT_SUBMIT),
            Map.entry("rest:POST /api/v1/projects/{projectId}/transfer-ownership", ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP),
            Map.entry("rest:POST /api/v1/projects/{projectId}/members", ProjectPolicyAction.PROJECT_MEMBER_ADD),
            Map.entry("rest:POST /api/v1/projects/{projectId}/publish", ProjectPolicyAction.PROJECT_PUBLISH),
            Map.entry("rest:PUT /api/v1/projects/{projectId}/part-quotas", ProjectPolicyAction.PROJECT_QUOTA_UPDATE),
            Map.entry("rest:DELETE /api/v1/projects/{projectId}", ProjectPolicyAction.PROJECT_DELETE),
            Map.entry("rest:POST /api/v1/projects/{projectId}/abort", ProjectPolicyAction.PROJECT_ABORT),
            Map.entry("rest:DELETE /api/v1/projects/{projectId}/members/{memberId}", ProjectPolicyAction.PROJECT_MEMBER_REMOVE),
            Map.entry("rest:PATCH /api/v1/projects/{projectId}/members/{memberId}/status", ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE),
            Map.entry("rest:GET /api/v1/projects", ProjectPolicyAction.PROJECT_LIST_PUBLIC),
            Map.entry("rest:GET /api/v1/projects/{projectId}", ProjectPolicyAction.PROJECT_READ),
            Map.entry("rest:GET /api/v1/projects/{projectId}/members", ProjectPolicyAction.PROJECT_MEMBER_LIST),
            Map.entry("rest:GET /api/v1/projects/members", ProjectPolicyAction.PROJECT_MEMBER_BATCH),
            Map.entry("rest:GET /api/v1/projects/me/managed", ProjectPolicyAction.PROJECT_LIST_MANAGED),
            Map.entry("rest:GET /api/v1/projects/me/draft", ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS),
            Map.entry("rest:POST /api/v1/projects/{projectId}/applications", ProjectPolicyAction.APPLICATION_CREATE),
            Map.entry("rest:PUT /api/v1/projects/{projectId}/applications/{applicationId}", ProjectPolicyAction.APPLICATION_UPDATE),
            Map.entry("rest:POST /api/v1/projects/{projectId}/applications/{applicationId}/submit", ProjectPolicyAction.APPLICATION_SUBMIT),
            Map.entry("rest:PATCH /api/v1/projects/{projectId}/applications/{applicationId}/decision", ProjectPolicyAction.APPLICATION_DECIDE),
            Map.entry("rest:DELETE /api/v1/projects/{projectId}/applications/{applicationId}", ProjectPolicyAction.APPLICATION_CANCEL),
            Map.entry("rest:GET /api/v1/projects/me/applications", ProjectPolicyAction.APPLICATION_LIST_SELF),
            Map.entry("rest:GET /api/v1/projects/applications", ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH),
            Map.entry("rest:GET /api/v1/projects/{projectId}/applications", ProjectPolicyAction.APPLICATION_LIST_PROJECT),
            Map.entry("rest:GET /api/v1/projects/{projectId}/applications/{applicationId}", ProjectPolicyAction.APPLICATION_READ),
            Map.entry("rest:PUT /api/v1/projects/{projectId}/application-form", ProjectPolicyAction.FORM_UPDATE),
            Map.entry("rest:GET /api/v1/projects/{projectId}/application-form", ProjectPolicyAction.FORM_READ),
            Map.entry("rest:GET /api/v1/project/matching-rounds", ProjectPolicyAction.MATCHING_LIST),
            Map.entry("rest:POST /api/v1/project/matching-rounds", ProjectPolicyAction.MATCHING_CREATE),
            Map.entry("rest:PATCH /api/v1/project/matching-rounds/{matchingRoundId}", ProjectPolicyAction.MATCHING_UPDATE),
            Map.entry("rest:DELETE /api/v1/project/matching-rounds/{matchingRoundId}", ProjectPolicyAction.MATCHING_DELETE),
            Map.entry("rest:POST /api/v1/project/matching-rounds/{matchingRoundId}/auto-decide", ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE),
            Map.entry("rest:GET /api/v1/projects/{projectId}/statistics", ProjectPolicyAction.STATISTICS_PROJECT),
            Map.entry("rest:GET /api/v1/projects/statistics", ProjectPolicyAction.STATISTICS_CHAPTER),
            Map.entry("rest:GET /api/v1/projects/statistics/matchings", ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING),
            Map.entry("rest:GET /api/v1/projects/permissions", ProjectPolicyAction.CAPABILITY_LIST),
            Map.entry("graphql:Query.project", ProjectPolicyAction.PROJECT_READ),
            Map.entry("graphql:Query.projects", ProjectPolicyAction.PROJECT_LIST_PUBLIC),
            Map.entry("graphql:Project.members", ProjectPolicyAction.PROJECT_MEMBER_LIST),
            Map.entry("graphql:Project.applicationForm", ProjectPolicyAction.FORM_READ),
            Map.entry("graphql:ProjectMember.application", ProjectPolicyAction.APPLICATION_READ),
            Map.entry("graphql:Project.productOwner", ProjectPolicyAction.PROJECT_READ),
            Map.entry("graphql:Project.coProductOwners", ProjectPolicyAction.PROJECT_READ),
            Map.entry("graphql:ProjectMember.member", ProjectPolicyAction.PROJECT_READ),
            Map.entry("scheduler:matching-round-deadline", ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE)
        );
    }

    private ProjectPolicyModule expectedModule(ProjectPolicyAction action) {
        if (Set.of(
            ProjectPolicyAction.PROJECT_LIST_PUBLIC,
            ProjectPolicyAction.PROJECT_LIST_MANAGED,
            ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS
        ).contains(action)) {
            return ProjectPolicyModule.PROJECT_SCOPE;
        }
        if (Set.of(
            ProjectPolicyAction.APPLICATION_LIST_SELF,
            ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH,
            ProjectPolicyAction.APPLICATION_LIST_PROJECT
        ).contains(action)) {
            return ProjectPolicyModule.APPLICATION_SCOPE;
        }
        if (action.name().startsWith("APPLICATION_")) {
            return ProjectPolicyModule.APPLICATION_RESOURCE;
        }
        if (action.name().startsWith("FORM_")) {
            return ProjectPolicyModule.FORM;
        }
        if (action.name().startsWith("MATCHING_")) {
            return ProjectPolicyModule.MATCHING_ROUND;
        }
        if (action.name().startsWith("STATISTICS_")) {
            return ProjectPolicyModule.STATISTICS;
        }
        return ProjectPolicyModule.PROJECT_RESOURCE;
    }
}
