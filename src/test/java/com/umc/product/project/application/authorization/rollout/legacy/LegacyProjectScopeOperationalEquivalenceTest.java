package com.umc.product.project.application.authorization.rollout.legacy;

import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.CHAPTER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.GISU_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.SCHOOL_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.internal;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.member;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.role;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.superAdminMember;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.surface;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.system;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationFormView;
import com.umc.product.project.domain.enums.ProjectStatus;

class LegacyProjectScopeOperationalEquivalenceTest {

    private final LegacyProjectAuthorizationAdapter adapter = new LegacyProjectAuthorizationAdapter();

    @Test
    @DisplayName("legacy public scope는 같은 기수 지부장에게 전체 기수, 일반 회원에게 공개 범위를 준다")
    void projectPublicScopePreservesChapterPresidentBreadth() {
        // Given
        var resource = ProjectPolicyResourceContext.builder().gisuScope(GISU_ID).build();

        // When
        var chapterPresident = evaluate(
            ProjectPolicyAction.PROJECT_LIST_PUBLIC,
            member(List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, 999L, GISU_ID)), List.of(), Map.of()),
            resource,
            internal()
        );
        var ordinary = evaluate(
            ProjectPolicyAction.PROJECT_LIST_PUBLIC,
            member(List.of(), List.of(), Map.of()),
            resource,
            internal()
        );

        // Then
        assertThat(chapterPresident.projectScope().gisuIds()).containsExactly(GISU_ID);
        assertThat(ordinary.projectScope().publicOnly()).isTrue();
    }

    @Test
    @DisplayName("legacy application scope는 학교 회장단의 같은 지부 프로젝트 목록을 허용한다")
    void applicationScopePreservesSchoolCoreAccess() {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)),
            List.of(),
            Map.of(new ProjectPolicySchoolChapterKey(GISU_ID, SCHOOL_ID), CHAPTER_ID)
        );
        var resource = projectResource();

        // When
        var allowed = evaluate(
            ProjectPolicyAction.APPLICATION_LIST_PROJECT, subject, resource, internal());
        var denied = evaluate(
            ProjectPolicyAction.APPLICATION_LIST_PROJECT,
            member(List.of(), List.of(), Map.of()), resource, internal());

        // Then
        assertThat(allowed.applicationScope().projectIds()).containsExactly(100L);
        assertThat(denied.effect()).isEqualTo(PolicyEffect.DENY);
    }

    @Test
    @DisplayName("legacy Form READ는 REST와 GraphQL에서 타 기수 SUPER_ADMIN을 다르게 처리한다")
    void formViewPreservesSurfaceSpecificSuperAdminBehavior() {
        // Given
        var subject = superAdminMember(List.of(), List.of(), Map.of());
        var resource = projectResource();

        // When
        var rest = evaluate(ProjectPolicyAction.FORM_READ, subject, resource,
            surface("rest:GET /api/v1/projects/{projectId}/application-form"));
        var graphQl = evaluate(ProjectPolicyAction.FORM_READ, subject, resource,
            surface("graphql:Project.applicationForm"));

        // Then
        assertThat(rest.formView()).isEqualTo(ProjectAuthorizationFormView.NONE);
        assertThat(graphQl.formView()).isEqualTo(ProjectAuthorizationFormView.FULL);
    }

    @Test
    @DisplayName("legacy statistics와 matching은 역할 기수 만료를 검사하지 않는다")
    void operationalRulesPreserveLooseGisuAndExpirySemantics() {
        // Given
        var central = member(
            List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, null, 99L)), List.of(), Map.of());
        var resource = ProjectPolicyResourceContext.builder()
            .matchingRound(300L, GISU_ID, CHAPTER_ID)
            .chapterScope(GISU_ID, CHAPTER_ID)
            .build();

        // When
        var statistics = evaluate(
            ProjectPolicyAction.STATISTICS_CHAPTER, central, resource, internal());
        var matching = evaluate(
            ProjectPolicyAction.MATCHING_CREATE, central, resource, internal());
        var outsider = evaluate(
            ProjectPolicyAction.MATCHING_CREATE,
            member(List.of(), List.of(), Map.of()), resource, internal());
        var scheduler = evaluate(
            ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            system("unverified-legacy-caller"), resource, internal());

        // Then
        assertThat(statistics.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(matching.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(outsider.effect()).isEqualTo(PolicyEffect.DENY);
        assertThat(scheduler.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    private ProjectAuthorizationDecision evaluate(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource,
        ProjectAuthorizationEvaluationPoint point
    ) {
        return (ProjectAuthorizationDecision) adapter.evaluate(
            new ProjectAuthorizationComparisonRequest(subject, action, resource, point));
    }

    private ProjectPolicyResourceContext projectResource() {
        return ProjectPolicyResourceContext.builder()
            .project(100L, GISU_ID, CHAPTER_ID, ProjectStatus.IN_PROGRESS)
            .productOwnerMemberId(999L)
            .build();
    }
}
