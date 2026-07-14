package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.service.AuthorizationService;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.project.application.authorization.ProjectSemanticActionBindings.Binding;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.evaluator.ProjectApplicationPermissionEvaluator;
import com.umc.product.project.application.service.evaluator.ProjectPermissionEvaluator;
import com.umc.product.project.application.service.evaluator.SuperAdminProperties;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectSemanticActionProductionPathContractTest {

    private static final long PROJECT_ID = 42L;
    private static final long APPLICATION_ID = 500L;
    private static final SubjectAttributes SUBJECT = new SubjectPolicyFacts(
        Instant.parse("2026-07-13T00:00:00Z"), List.of(), List.of(), Map.of()
    ).toSubjectAttributes(99L, 1L);

    private final LoadProjectPort loadProjectPort = mock(LoadProjectPort.class);
    private final LoadProjectApplicationPort loadProjectApplicationPort = mock(LoadProjectApplicationPort.class);
    private final LoadProjectMemberPort loadProjectMemberPort = mock(LoadProjectMemberPort.class);
    private final ProjectPolicyAuthorizationService policyService = mock(ProjectPolicyAuthorizationService.class);

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        Project project = mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getGisuId()).willReturn(1L);
        given(project.getChapterId()).willReturn(10L);
        given(project.getStatus()).willReturn(ProjectStatus.DRAFT);
        given(project.getCreatorMemberId()).willReturn(99L);
        given(project.getProductOwnerMemberId()).willReturn(99L);
        given(loadProjectPort.findById(PROJECT_ID)).willReturn(Optional.of(project));

        ProjectApplicationForm form = mock(ProjectApplicationForm.class);
        given(form.getProject()).willReturn(project);
        ProjectApplication application = mock(ProjectApplication.class);
        given(application.getId()).willReturn(APPLICATION_ID);
        given(application.getApplicationForm()).willReturn(form);
        given(application.getStatus()).willReturn(ProjectApplicationStatus.DRAFT);
        given(application.getApplicantMemberId()).willReturn(99L);
        given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        PolicyDecision allow = mock(PolicyDecision.class);
        given(allow.effect()).willReturn(PolicyEffect.ALLOW);
        given(policyService.evaluate(
            eq(SUBJECT), any(ProjectPolicyAction.class), any(ProjectPolicyResourceContext.class)
        )).willReturn(allow);
        given(policyService.allowsAny(
            eq(SUBJECT), anyList(), any(ProjectPolicyResourceContext.class)
        )).willReturn(true);

        ProjectPermissionEvaluator projectEvaluator = new ProjectPermissionEvaluator(
            loadProjectPort, policyService, new SuperAdminProperties(false));
        ProjectApplicationPermissionEvaluator applicationEvaluator =
            new ProjectApplicationPermissionEvaluator(
                loadProjectPort,
                loadProjectApplicationPort,
                loadProjectMemberPort,
                policyService,
                new SuperAdminProperties(false)
            );
        authorizationService = new AuthorizationService(
            mock(GetChallengerRoleUseCase.class),
            List.of(projectEvaluator, applicationEvaluator),
            mock(GetMemberUseCase.class),
            mock(GetChapterUseCase.class),
            mock(GetChallengerUseCase.class),
            mock(GetGisuUseCase.class),
            mock(OperationalMetrics.class),
            Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("semanticBindings")
    @DisplayName("20개 semantic action은 실제 AuthorizationService와 evaluator registry에서 exact action으로 평가된다")
    void semanticAction이_production_path에서_exact_action으로_평가된다(Binding binding) {
        ResourcePermission permission = resourcePermission(binding);

        boolean allowed = authorizationService.check(SUBJECT, permission, binding.action().id());

        assertThat(allowed).isTrue();
        assertExactPolicySink(binding.action());
    }

    private void assertExactPolicySink(ProjectPolicyAction expectedAction) {
        if (expectedAction == ProjectPolicyAction.PROJECT_MEMBER_BATCH
            || expectedAction == ProjectPolicyAction.APPLICATION_CREATE) {
            ArgumentCaptor<ProjectPolicyAction> action = ArgumentCaptor.forClass(ProjectPolicyAction.class);
            verify(policyService).evaluate(
                eq(SUBJECT), action.capture(), any(ProjectPolicyResourceContext.class));
            assertThat(action.getValue()).isEqualTo(expectedAction);
            return;
        }
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectPolicyAction>> actions = ArgumentCaptor.forClass(List.class);
        verify(policyService).allowsAny(
            eq(SUBJECT), actions.capture(), any(ProjectPolicyResourceContext.class));
        assertThat(actions.getValue()).containsExactly(expectedAction);
    }

    private static ResourcePermission resourcePermission(Binding binding) {
        long resourceId = binding.action() == ProjectPolicyAction.PROJECT_MEMBER_BATCH
            ? PROJECT_ID
            : binding.resourceType() == com.umc.product.authorization.domain.ResourceType.PROJECT
                || binding.action() == ProjectPolicyAction.APPLICATION_CREATE
                ? PROJECT_ID
                : APPLICATION_ID;
        return ResourcePermission.of(binding.resourceType(), resourceId, binding.permissionType());
    }

    private static Stream<Arguments> semanticBindings() {
        return ProjectSemanticActionBindings.values().stream().map(Arguments::of);
    }
}
