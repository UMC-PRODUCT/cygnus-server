package com.umc.product.project.application.authorization.rollout.legacy;

import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.CHAPTER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.GISU_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.MEMBER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.OTHER_MEMBER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.SCHOOL_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.challenger;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.internal;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.member;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.role;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.superAdminMember;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class LegacyProjectResourceAuthorizationEquivalenceTest {

    private final LegacyProjectAuthorizationAdapter adapter = new LegacyProjectAuthorizationAdapter();

    @ParameterizedTest(name = "{0}은 DRAFT creator를 허용한다")
    @MethodSource("projectEditActions")
    void projectEditFamilyAllowsDraftCreator(ProjectPolicyAction action) {
        // Given
        var resource = project(ProjectStatus.DRAFT).creatorMemberId(MEMBER_ID).build();

        // When
        ProjectAuthorizationDecision decision = evaluate(
            action, member(List.of(), List.of(), Map.of()), resource);

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @ParameterizedTest(name = "{0}은 DRAFT 타인을 거부한다")
    @MethodSource("projectEditActions")
    void projectEditFamilyDeniesDraftNonCreator(ProjectPolicyAction action) {
        // Given
        var resource = project(ProjectStatus.DRAFT).creatorMemberId(OTHER_MEMBER_ID).build();

        // When
        ProjectAuthorizationDecision decision = evaluate(
            action, member(List.of(), List.of(), Map.of()), resource);

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.DENY);
    }

    @ParameterizedTest(name = "{0}은 만료된 같은 지부 지부장을 허용한다")
    @MethodSource("projectManageActions")
    void projectManageFamilyAllowsExpiredChapterPresident(ProjectPolicyAction action) {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, CHAPTER_ID, GISU_ID)),
            List.of(), Map.of());

        // When
        ProjectAuthorizationDecision decision = evaluate(
            action, subject, project(ProjectStatus.PENDING_REVIEW).build());

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @ParameterizedTest(name = "{0}은 일반 사용자를 거부한다")
    @MethodSource("projectManageActions")
    void projectManageFamilyDeniesOrdinaryMember(ProjectPolicyAction action) {
        // Given
        var subject = member(List.of(), List.of(), Map.of());

        // When
        ProjectAuthorizationDecision decision = evaluate(
            action, subject, project(ProjectStatus.PENDING_REVIEW).build());

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.DENY);
    }

    @Test
    @DisplayName("legacy Project CREATE actor gate는 다른 기수의 PLAN 이력도 허용한다")
    void projectCreateAllowsPlanHistoryFromAnotherGisu() {
        // Given
        var subject = member(List.of(), List.of(challenger(99L, ChallengerPart.PLAN)), Map.of());
        var resource = project(ProjectStatus.DRAFT).creatorMemberId(MEMBER_ID).build();

        // When
        ProjectAuthorizationDecision decision = evaluate(ProjectPolicyAction.PROJECT_CREATE, subject, resource);

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    @DisplayName("legacy delegated Project CREATE는 target 학교 fact가 없으면 typed failure다")
    void delegatedProjectCreateFailsWithoutTargetSchoolFact() {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)),
            List.of(), Map.of());
        var resource = project(ProjectStatus.DRAFT).creatorMemberId(OTHER_MEMBER_ID).build();
        var request = new ProjectAuthorizationComparisonRequest(
            subject, ProjectPolicyAction.PROJECT_CREATE, resource, internal());

        // When
        Object result = adapter.evaluate(request);

        // Then
        assertThat(result).isInstanceOf(ProjectAuthorizationEvaluationFailure.class);
    }

    @Test
    @DisplayName("legacy delegated Project CREATE는 같은 학교만 허용한다")
    void delegatedProjectCreateRequiresExactSchool() {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)),
            List.of(), Map.of());
        var resource = project(ProjectStatus.DRAFT).creatorMemberId(OTHER_MEMBER_ID).build();

        // When
        ProjectAuthorizationDecision decision = evaluate(
            ProjectPolicyAction.PROJECT_CREATE,
            subject,
            ProjectAuthorizationResourceSnapshot.withTargetMemberSchool(resource, SCHOOL_ID)
        );

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @ParameterizedTest(name = "{0}은 applicant의 DRAFT를 허용한다")
    @MethodSource("applicationEditActions")
    void applicationEditFamilyAllowsApplicantDraft(ProjectPolicyAction action) {
        // Given
        var resource = application(ProjectApplicationStatus.DRAFT, MEMBER_ID);

        // When
        ProjectAuthorizationDecision decision = evaluate(
            action, member(List.of(), List.of(), Map.of()), resource);

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    @DisplayName("legacy SUPER_ADMIN application decide는 enforcement ALLOW지만 capability는 DENY다")
    void applicationDecidePreservesCapabilityMismatch() {
        // Given
        var subject = superAdminMember(List.of(), List.of(), Map.of());
        var resource = application(ProjectApplicationStatus.SUBMITTED, OTHER_MEMBER_ID);

        // When
        ProjectAuthorizationDecision decision = evaluate(
            ProjectPolicyAction.APPLICATION_DECIDE, subject, resource);

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(decision.forceDecision()).isTrue();
        assertThat(decision.capabilityAuthorized()).isFalse();
    }

    private ProjectAuthorizationDecision evaluate(
        ProjectPolicyAction action,
        com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return evaluate(action, subject, ProjectAuthorizationResourceSnapshot.of(resource));
    }

    private ProjectAuthorizationDecision evaluate(
        ProjectPolicyAction action,
        com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot subject,
        ProjectAuthorizationResourceSnapshot resource
    ) {
        return (ProjectAuthorizationDecision) adapter.evaluate(new ProjectAuthorizationComparisonRequest(
            subject, action, resource, internal()));
    }

    private ProjectPolicyResourceContext.Builder project(ProjectStatus status) {
        return ProjectPolicyResourceContext.builder()
            .project(100L, GISU_ID, CHAPTER_ID, status)
            .productOwnerMemberId(OTHER_MEMBER_ID);
    }

    private ProjectPolicyResourceContext application(
        ProjectApplicationStatus status,
        long applicantMemberId
    ) {
        return project(ProjectStatus.IN_PROGRESS)
            .application(200L, status, applicantMemberId)
            .build();
    }

    private static Stream<ProjectPolicyAction> projectEditActions() {
        return Stream.of(
            ProjectPolicyAction.PROJECT_UPDATE,
            ProjectPolicyAction.PROJECT_SUBMIT,
            ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP,
            ProjectPolicyAction.PROJECT_MEMBER_ADD,
            ProjectPolicyAction.PROJECT_MEMBER_REMOVE,
            ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE,
            ProjectPolicyAction.FORM_UPDATE
        );
    }

    private static Stream<ProjectPolicyAction> projectManageActions() {
        return Stream.of(
            ProjectPolicyAction.PROJECT_PUBLISH,
            ProjectPolicyAction.PROJECT_QUOTA_UPDATE,
            ProjectPolicyAction.PROJECT_ABORT
        );
    }

    private static Stream<ProjectPolicyAction> applicationEditActions() {
        return Stream.of(ProjectPolicyAction.APPLICATION_UPDATE, ProjectPolicyAction.APPLICATION_SUBMIT);
    }
}
