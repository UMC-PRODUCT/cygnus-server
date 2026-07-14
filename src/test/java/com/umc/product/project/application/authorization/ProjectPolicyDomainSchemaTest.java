package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

class ProjectPolicyDomainSchemaTest {

    @Test
    @DisplayName("project-1.0은 39개 canonical action을 optional attribute 없이 등록한다")
    void registersExactCanonicalActionsWithoutOptionalAttributes() {
        PolicyDomainSchema schema = ProjectPolicyDomainSchema.create();

        assertThat(schema.contextSchemaVersion()).isEqualTo("project-1.0");
        for (ProjectPolicyAction action : ProjectPolicyAction.values()) {
            assertThat(schema.action(action.id())).as(action.id()).isPresent();
            assertThat(schema.action(action.id()).orElseThrow().optionalAttributes()).isEmpty();
        }
    }

    @Test
    @DisplayName("form.view dominance와 scope outcome 계약을 정확히 등록한다")
    void registersOutcomeContracts() {
        PolicyDomainSchema schema = ProjectPolicyDomainSchema.create();

        assertThat(schema.outcome(ProjectPolicyOutcomes.FORM_VIEW).orElseThrow().dominanceOrder())
            .containsExactly("FULL", "APPLICANT", "NONE");
        assertThat(schema.action(ProjectPolicyAction.PROJECT_LIST_MANAGED.id()).orElseThrow().allowedOutcomes())
            .containsExactlyInAnyOrder(
                ProjectPolicyOutcomes.PROJECT_ALL,
                ProjectPolicyOutcomes.PROJECT_GISU_IDS,
                ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS,
                ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS,
                ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS
            );
    }
}
