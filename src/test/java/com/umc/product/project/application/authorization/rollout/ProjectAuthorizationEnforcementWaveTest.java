package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationEnforcementWaveTest {

    @Test
    @DisplayName("네 enforcement wave는 6, 7, 20, 6개 action으로 정확히 분할한다")
    void enforcement_wave는_39개_action을_정확히_분할한다() {
        assertThat(ProjectAuthorizationEnforcementWave.READ_CAPABILITY.actions())
            .containsExactlyInAnyOrder(
                ProjectPolicyAction.PROJECT_READ,
                ProjectPolicyAction.PROJECT_MEMBER_LIST,
                ProjectPolicyAction.PROJECT_MEMBER_BATCH,
                ProjectPolicyAction.APPLICATION_READ,
                ProjectPolicyAction.FORM_READ,
                ProjectPolicyAction.CAPABILITY_LIST
            );
        assertThat(ProjectAuthorizationEnforcementWave.LIST_GRAPHQL.actions())
            .containsExactlyInAnyOrder(
                ProjectPolicyAction.PROJECT_LIST_PUBLIC,
                ProjectPolicyAction.PROJECT_LIST_MANAGED,
                ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS,
                ProjectPolicyAction.APPLICATION_LIST_SELF,
                ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH,
                ProjectPolicyAction.APPLICATION_LIST_PROJECT,
                ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT
            );
        assertThat(ProjectAuthorizationEnforcementWave.COMMAND_STATISTICS.actions())
            .containsExactlyInAnyOrder(
                ProjectPolicyAction.PROJECT_CREATE,
                ProjectPolicyAction.PROJECT_UPDATE,
                ProjectPolicyAction.PROJECT_SUBMIT,
                ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP,
                ProjectPolicyAction.PROJECT_MEMBER_ADD,
                ProjectPolicyAction.PROJECT_PUBLISH,
                ProjectPolicyAction.PROJECT_QUOTA_UPDATE,
                ProjectPolicyAction.PROJECT_DELETE,
                ProjectPolicyAction.PROJECT_ABORT,
                ProjectPolicyAction.PROJECT_MEMBER_REMOVE,
                ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE,
                ProjectPolicyAction.APPLICATION_CREATE,
                ProjectPolicyAction.APPLICATION_UPDATE,
                ProjectPolicyAction.APPLICATION_SUBMIT,
                ProjectPolicyAction.APPLICATION_DECIDE,
                ProjectPolicyAction.APPLICATION_CANCEL,
                ProjectPolicyAction.FORM_UPDATE,
                ProjectPolicyAction.STATISTICS_PROJECT,
                ProjectPolicyAction.STATISTICS_CHAPTER,
                ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING
            );
        assertThat(ProjectAuthorizationEnforcementWave.MATCHING_SCHEDULER.actions())
            .containsExactlyInAnyOrder(
                ProjectPolicyAction.MATCHING_LIST,
                ProjectPolicyAction.MATCHING_CREATE,
                ProjectPolicyAction.MATCHING_UPDATE,
                ProjectPolicyAction.MATCHING_DELETE,
                ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE,
                ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE
            );

        Set<ProjectPolicyAction> union = EnumSet.noneOf(ProjectPolicyAction.class);
        for (ProjectAuthorizationEnforcementWave wave : ProjectAuthorizationEnforcementWave.values()) {
            assertThat(union).doesNotContainAnyElementsOf(wave.actions());
            union.addAll(wave.actions());
        }
        assertThat(union).containsExactlyInAnyOrder(ProjectPolicyAction.values());
    }
}
