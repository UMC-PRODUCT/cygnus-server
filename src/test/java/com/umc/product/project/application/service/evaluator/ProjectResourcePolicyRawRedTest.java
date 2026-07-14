package com.umc.product.project.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectResourcePolicyRawRedTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T00:00:00Z");
    @Mock
    LoadProjectPort loadProjectPort;

    @Test
    void 만료된_중앙운영진은_private_READ와_broad_WRITE가_모두_DENY다() {
        Project project = project(100L, 1L, 10L, ProjectStatus.PENDING_REVIEW);
        given(loadProjectPort.findById(100L)).willReturn(Optional.of(project));
        ProjectPolicySubjectSnapshot expiredSubject = snapshot(END, List.of(roleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 1L)), List.of());
        ProjectPermissionEvaluator evaluator = projectEvaluator(expiredSubject);
        SubjectAttributes expiredRole = subject(20L, List.of(), List.of(roleAttribute(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 1L)));
        boolean readAllowed = evaluator.evaluate(
            expiredRole,
            ResourcePermission.of(ResourceType.PROJECT, 100L, PermissionType.READ));

        assertThat(readAllowed).isFalse();
        assertThat(evaluator.evaluate(
            expiredRole,
            ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE)
        )).isFalse();
    }

    @Test
    void 같은_기수의_다른_지부장_private_READ는_DENY다() {
        Project project = project(100L, 10L, 10L, ProjectStatus.PENDING_REVIEW);
        given(loadProjectPort.findById(100L)).willReturn(Optional.of(project));
        ProjectPolicySubjectSnapshot otherChapterPresident = snapshot(START, List.of(roleTuple(
            ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 20L, 1L)), List.of());
        boolean actual = projectEvaluator(otherChapterPresident).evaluate(
            subject(20L, List.of(), List.of(roleAttribute(
                ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 20L, 1L))),
            ResourcePermission.of(ResourceType.PROJECT, 100L, PermissionType.READ));

        assertThat(actual).isFalse();
    }

    @Test
    void 다른_기수_PLAN의_PROJECT_CREATE는_DENY다() {
        ProjectPolicySubjectSnapshot otherGisuPlan =
            snapshot(START, List.of(), List.of(challengerTuple(1L, 10L)));
        PolicyDecision decision = ProjectPolicyEvaluatorTestSupport.authorizationService(otherGisuPlan).evaluate(
            otherGisuPlan,
            ProjectPolicyAction.PROJECT_CREATE,
            ProjectPolicyResourceContext.builder()
                .projectTarget(2L, 10L)
                .creatorMemberId(20L)
                .build()
        );

        assertThat(decision.effect()).isEqualTo(PolicyEffect.DENY);
    }

    @Test
    void active_SUPER_ADMIN의_decide_outcome과_capability는_같이_ALLOW여야_한다() {
        Project project = project(100L, 10L, 10L, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot superAdmin = snapshot(START, true, List.of(), List.of());
        PolicyDecision target = ProjectPolicyEvaluatorTestSupport.authorizationService(superAdmin).evaluate(
            superAdmin,
            ProjectPolicyAction.APPLICATION_DECIDE,
            ProjectPolicyResourceContext.builder()
                .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
                .creatorMemberId(project.getCreatorMemberId())
                .productOwnerMemberId(project.getProductOwnerMemberId())
                .application(200L, ProjectApplicationStatus.SUBMITTED, 30L)
                .build()
        );
        boolean forceDecision = target.outcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION)
            .filter(new PolicyValue.BooleanValue(true)::equals)
            .isPresent();

        assertThat(forceDecision).isTrue();
        assertThat(target.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    void 필수_context가_누락되면_POLICY_EVALUATION_FAILED로_변환한다() {
        ProjectPolicySubjectSnapshot plan =
            snapshot(START, List.of(), List.of(challengerTuple(1L, 10L)));

        assertThatThrownBy(() -> ProjectPolicyEvaluatorTestSupport.authorizationService(plan).evaluate(
            plan,
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder().build()
        ))
            .isInstanceOf(AuthorizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
    }

    private ProjectPermissionEvaluator projectEvaluator(ProjectPolicySubjectSnapshot snapshot) {
        return new ProjectPermissionEvaluator(
            loadProjectPort,
            ProjectPolicyEvaluatorTestSupport.authorizationService(snapshot),
            new SuperAdminProperties(false)
        );
    }

    private ProjectPolicySubjectSnapshot snapshot(
        Instant evaluatedAt,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers
    ) {
        return snapshot(evaluatedAt, false, roles, challengers);
    }

    private ProjectPolicySubjectSnapshot snapshot(
        Instant evaluatedAt,
        boolean superAdmin,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(20L), evaluatedAt, superAdmin, roles, challengers, Map.of());
    }

    private ProjectPolicyRoleTuple roleTuple(
        ChallengerRoleType type,
        OrganizationType organizationType,
        Long organizationId,
        long gisuId
    ) {
        return new ProjectPolicyRoleTuple(type, organizationType, organizationId, null, gisuId, START, END);
    }

    private ProjectPolicyChallengerTuple challengerTuple(long gisuId, long chapterId) {
        return new ProjectPolicyChallengerTuple(500L, gisuId, chapterId, ChallengerPart.PLAN, START, END);
    }

    private Project project(Long id, Long chapterId, Long ownerId, ProjectStatus status) {
        Project project = Project.createDraft(1L, chapterId, ownerId, 1L, ownerId);
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "status", status);
        return project;
    }

    private SubjectAttributes subject(
        Long memberId,
        List<GisuChallengerInfo> challengers,
        List<RoleAttribute> roles
    ) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(1L)
            .gisuChallengerInfos(challengers)
            .roleAttributes(roles)
            .build();
    }

    private RoleAttribute roleAttribute(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        Long gisuId
    ) {
        return new RoleAttribute(roleType, organizationType, organizationId, null, gisuId);
    }

}
