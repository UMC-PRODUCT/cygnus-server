package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectPolicyCanonicalContractTest {

    @Test
    @DisplayName("Project action ID는 사용자 승인 canonical 계약과 정확히 일치한다")
    void actionIdsMatchApprovedCanonicalContract() {
        Map<ProjectPolicyAction, String> expected = Map.ofEntries(
            Map.entry(ProjectPolicyAction.PROJECT_CREATE, "project:create"),
            Map.entry(ProjectPolicyAction.PROJECT_UPDATE, "project:update-info"),
            Map.entry(ProjectPolicyAction.PROJECT_SUBMIT, "project:submit-review"),
            Map.entry(ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP, "project:transfer-ownership"),
            Map.entry(ProjectPolicyAction.PROJECT_MEMBER_ADD, "project-member:add"),
            Map.entry(ProjectPolicyAction.PROJECT_PUBLISH, "project:publish"),
            Map.entry(ProjectPolicyAction.PROJECT_QUOTA_UPDATE, "project:update-part-quota"),
            Map.entry(ProjectPolicyAction.PROJECT_DELETE, "project:delete"),
            Map.entry(ProjectPolicyAction.PROJECT_ABORT, "project:abort"),
            Map.entry(ProjectPolicyAction.PROJECT_MEMBER_REMOVE, "project-member:remove"),
            Map.entry(ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE, "project-member:change-status"),
            Map.entry(ProjectPolicyAction.PROJECT_LIST_PUBLIC, "project:list-public"),
            Map.entry(ProjectPolicyAction.PROJECT_READ, "project:read"),
            Map.entry(ProjectPolicyAction.PROJECT_MEMBER_LIST, "project-member:list"),
            Map.entry(ProjectPolicyAction.PROJECT_MEMBER_BATCH, "project-member:batch"),
            Map.entry(ProjectPolicyAction.PROJECT_LIST_MANAGED, "project:list-managed"),
            Map.entry(ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS, "project:list-own-drafts"),
            Map.entry(ProjectPolicyAction.APPLICATION_CREATE, "project-application:create"),
            Map.entry(ProjectPolicyAction.APPLICATION_UPDATE, "project-application:update"),
            Map.entry(ProjectPolicyAction.APPLICATION_SUBMIT, "project-application:submit"),
            Map.entry(ProjectPolicyAction.APPLICATION_DECIDE, "project-application:decide"),
            Map.entry(ProjectPolicyAction.APPLICATION_CANCEL, "project-application:cancel"),
            Map.entry(ProjectPolicyAction.APPLICATION_LIST_SELF, "project-application:list-self"),
            Map.entry(ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH, "project-application:list-project-batch"),
            Map.entry(ProjectPolicyAction.APPLICATION_LIST_PROJECT, "project-application:list-project"),
            Map.entry(ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT, "project-application:list-management"),
            Map.entry(ProjectPolicyAction.APPLICATION_READ, "project-application:read"),
            Map.entry(ProjectPolicyAction.FORM_UPDATE, "project-form:update"),
            Map.entry(ProjectPolicyAction.FORM_READ, "project-form:read"),
            Map.entry(ProjectPolicyAction.MATCHING_LIST, "project-matching-round:list"),
            Map.entry(ProjectPolicyAction.MATCHING_CREATE, "project-matching-round:create"),
            Map.entry(ProjectPolicyAction.MATCHING_UPDATE, "project-matching-round:update"),
            Map.entry(ProjectPolicyAction.MATCHING_DELETE, "project-matching-round:delete"),
            Map.entry(ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE, "project-matching-round:human-auto-decide"),
            Map.entry(ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE, "project-matching-round:system-auto-decide"),
            Map.entry(ProjectPolicyAction.STATISTICS_PROJECT, "project-statistics:read-project"),
            Map.entry(ProjectPolicyAction.STATISTICS_CHAPTER, "project-statistics:read-chapter"),
            Map.entry(ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING, "project-statistics:read-public-matching"),
            Map.entry(ProjectPolicyAction.CAPABILITY_LIST, "project:capability-list")
        );

        assertThat(ProjectPolicyAction.values()).hasSize(39);
        assertThat(expected).hasSize(39);
        expected.forEach((action, id) -> assertThat(action.id()).as(action.name()).isEqualTo(id));
    }

    @Test
    @DisplayName("46개 실행 surface와 explicit non-surface action의 합은 전체 action catalog다")
    void surfaceAndExplicitNonSurfaceActionsCoverTheCatalog() {
        Set<ProjectPolicyAction> surfaceActions = ProjectPolicySurfaceCatalog.surfaces().stream()
            .map(ProjectPolicySurfaceDescriptor::action)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(ProjectPolicyAction.nonSurfaceActions())
            .containsExactly(ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT);
        assertThat(surfaceActions).doesNotContain(ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT);
        assertThat(java.util.stream.Stream.concat(
                surfaceActions.stream(), ProjectPolicyAction.nonSurfaceActions().stream())
            .collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(ProjectPolicyAction.values());
    }

    @Test
    @DisplayName("고정 classpath 경로에 Project policy bundle이 존재한다")
    void approvedPolicyBundleExistsOnClasspath() throws Exception {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("policies/project/bundle.json")) {
            assertThat(resource).isNotNull();
        }
    }
}
