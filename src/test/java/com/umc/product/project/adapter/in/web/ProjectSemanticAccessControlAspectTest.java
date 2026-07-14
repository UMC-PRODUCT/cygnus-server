package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.authorization.adapter.in.aspect.AccessControlAspect;
import com.umc.product.authorization.adapter.in.aspect.AuthorizationRequestSubjectContext;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.service.AuthorizationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshotLoader;
import com.umc.product.project.application.authorization.rollout.ConfiguredProjectAuthorizationRolloutModeResolver;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassifier;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationResult;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutConfiguration;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutCoordinator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutMode;
import com.umc.product.project.application.port.in.command.AbortProjectUseCase;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.CancelProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.ChangeProjectMemberStatusUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.DecideApplicationUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.RemoveProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.TransferProjectOwnershipUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectApplicationDraftUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.evaluator.ProjectPermissionEvaluator;
import com.umc.product.project.application.service.evaluator.SuperAdminProperties;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@WebMvcTest(controllers = {ProjectCommandController.class, ProjectApplicationController.class})
@Import({
    AccessControlAspect.class,
    AuthorizationRequestSubjectContext.class,
    JacksonConfig.class,
    ProjectSemanticAccessControlAspectTest.AspectProxyTestConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("프로젝트 semantic @CheckAccess 실제 적용")
class ProjectSemanticAccessControlAspectTest {

    private static final Long MEMBER_ID = 99L;
    private static final Long PROJECT_ID = 42L;
    private static final Long APPLICATION_ID = 500L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final SubjectAttributes SUBJECT = new SubjectPolicyFacts(
        EVALUATED_AT, List.of(), List.of(), Map.of()
    ).toSubjectAttributes(MEMBER_ID, 1L);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CreateDraftProjectUseCase createDraftProjectUseCase;
    @MockitoBean
    UpdateProjectUseCase updateProjectUseCase;
    @MockitoBean
    SubmitProjectUseCase submitProjectUseCase;
    @MockitoBean
    TransferProjectOwnershipUseCase transferProjectOwnershipUseCase;
    @MockitoBean
    AddProjectMemberUseCase addProjectMemberUseCase;
    @MockitoBean
    RemoveProjectMemberUseCase removeProjectMemberUseCase;
    @MockitoBean
    ChangeProjectMemberStatusUseCase changeProjectMemberStatusUseCase;
    @MockitoBean
    UpdatePartQuotasUseCase updatePartQuotasUseCase;
    @MockitoBean
    PublishProjectUseCase publishProjectUseCase;
    @MockitoBean
    DeleteProjectUseCase deleteProjectUseCase;
    @MockitoBean
    AbortProjectUseCase abortProjectUseCase;

    @MockitoBean
    CreateDraftProjectApplicationUseCase createDraftProjectApplicationUseCase;
    @MockitoBean
    UpdateProjectApplicationDraftUseCase updateProjectApplicationDraftUseCase;
    @MockitoBean
    SubmitProjectApplicationUseCase submitProjectApplicationUseCase;
    @MockitoBean
    DecideApplicationUseCase decideApplicationUseCase;
    @MockitoBean
    CancelProjectApplicationUseCase cancelProjectApplicationUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        clearInvocations(checkPermissionUseCase, submitProjectUseCase, submitProjectApplicationUseCase);
        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(SUBJECT);
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(MEMBER_ID)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Nested
    @DisplayName("프로젝트 제출")
    class ProjectSubmit {

        private static final String ACTION = "project:submit-review";

        @Test
        @DisplayName("semantic 권한이 허용되면 제출 UseCase를 한 번 호출한다")
        void 허용이면_프로젝트_제출_UseCase를_호출한다() throws Exception {
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willReturn(true);

            mockMvc.perform(post("/api/v1/projects/{projectId}/submit", PROJECT_ID))
                .andExpect(status().isOk());

            ArgumentCaptor<ResourcePermission> permissionCaptor =
                ArgumentCaptor.forClass(ResourcePermission.class);
            then(checkPermissionUseCase).should().check(eq(SUBJECT), permissionCaptor.capture(), eq(ACTION));
            assertThat(permissionCaptor.getValue()).isEqualTo(
                ResourcePermission.of(ResourceType.PROJECT, PROJECT_ID, PermissionType.EDIT));
            then(submitProjectUseCase).should(times(1)).submit(any());
        }

        @Test
        @DisplayName("semantic 권한이 거부되면 403이고 제출 UseCase를 호출하지 않는다")
        void 거부이면_프로젝트_제출_부수효과가_없다() throws Exception {
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willReturn(false);

            mockMvc.perform(post("/api/v1/projects/{projectId}/submit", PROJECT_ID))
                .andExpect(status().isForbidden());

            then(checkPermissionUseCase).should().check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION));
            then(submitProjectUseCase).should(never()).submit(any());
        }

        @Test
        @DisplayName("정책 평가 실패면 500이고 제출 UseCase를 호출하지 않는다")
        void 정책_평가_실패이면_프로젝트_제출_부수효과가_없다() throws Exception {
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willThrow(new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED));

            mockMvc.perform(post("/api/v1/projects/{projectId}/submit", PROJECT_ID))
                .andExpect(status().isInternalServerError());

            then(checkPermissionUseCase).should().check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION));
            then(submitProjectUseCase).should(never()).submit(any());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.umc.product.project.adapter.in.web.ProjectSemanticAccessControlAspectTest#rolloutCases")
        @DisplayName("실제 aspect와 rollout coordinator는 authoritative mode별로 UseCase를 정확히 0회 또는 1회 실행한다")
        void 실제_rollout_mode가_프로젝트_제출_부수효과를_결정한다(RolloutCase rolloutCase)
            throws Exception {
            ProjectPolicyAuthorizationService policyService = policyService(rolloutCase);
            SemanticAuthorization semanticAuthorization = semanticAuthorization(policyService);
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willAnswer(invocation -> semanticAuthorization.authorizationService().check(
                    invocation.<SubjectAttributes>getArgument(0),
                    invocation.<ResourcePermission>getArgument(1),
                    invocation.<String>getArgument(2)));

            mockMvc.perform(post("/api/v1/projects/{projectId}/submit", PROJECT_ID))
                .andExpect(status().is(rolloutCase.expectedStatus()));

            then(semanticAuthorization.loadProjectPort()).should().findById(PROJECT_ID);
            then(submitProjectUseCase).should(times(rolloutCase.expectedExecutions())).submit(any());
        }
    }

    @Nested
    @DisplayName("프로젝트 지원서 제출")
    class ApplicationSubmit {

        private static final String ACTION = "project-application:submit";

        @Test
        @DisplayName("semantic 권한이 허용되면 지원서 제출 UseCase를 한 번 호출한다")
        void 허용이면_지원서_제출_UseCase를_호출한다() throws Exception {
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willReturn(true);
            given(submitProjectApplicationUseCase.submit(any()))
                .willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.SUBMITTED));

            mockMvc.perform(post(
                    "/api/v1/projects/{projectId}/applications/{applicationId}/submit",
                    PROJECT_ID,
                    APPLICATION_ID
                ))
                .andExpect(status().isOk());

            ArgumentCaptor<ResourcePermission> permissionCaptor =
                ArgumentCaptor.forClass(ResourcePermission.class);
            then(checkPermissionUseCase).should().check(eq(SUBJECT), permissionCaptor.capture(), eq(ACTION));
            assertThat(permissionCaptor.getValue()).isEqualTo(
                ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.EDIT));
            then(submitProjectApplicationUseCase).should(times(1)).submit(any());
        }

        @Test
        @DisplayName("semantic 권한이 거부되면 403이고 지원서 제출 UseCase를 호출하지 않는다")
        void 거부이면_지원서_제출_부수효과가_없다() throws Exception {
            given(checkPermissionUseCase.check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION)))
                .willReturn(false);

            mockMvc.perform(post(
                    "/api/v1/projects/{projectId}/applications/{applicationId}/submit",
                    PROJECT_ID,
                    APPLICATION_ID
                ))
                .andExpect(status().isForbidden());

            then(checkPermissionUseCase).should().check(eq(SUBJECT), any(ResourcePermission.class), eq(ACTION));
            then(submitProjectApplicationUseCase).should(never()).submit(any());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectProxyTestConfiguration {
    }

    private static Stream<Arguments> rolloutCases() {
        return Stream.of(
            rollout("LEGACY allow", mode(ProjectAuthorizationRolloutMode.LEGACY), allow(), deny(), 200, 1),
            rollout("LEGACY deny", mode(ProjectAuthorizationRolloutMode.LEGACY), deny(), allow(), 403, 0),
            rollout("LEGACY failure", mode(ProjectAuthorizationRolloutMode.LEGACY), failure(), allow(), 500, 0),
            rollout("SHADOW legacy allow target deny", mode(ProjectAuthorizationRolloutMode.SHADOW),
                allow(), deny(), 200, 1),
            rollout("SHADOW target failure", mode(ProjectAuthorizationRolloutMode.SHADOW),
                allow(), failure(), 200, 1),
            rollout("SHADOW legacy failure", mode(ProjectAuthorizationRolloutMode.SHADOW),
                failure(), allow(), 500, 0),
            rollout("ENFORCE target allow", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                deny(), allow(), 200, 1),
            rollout("ENFORCE legacy failure target allow", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                failure(), allow(), 200, 1),
            rollout("ENFORCE target deny", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                allow(), deny(), 403, 0),
            rollout("ENFORCE target failure", mode(ProjectAuthorizationRolloutMode.ENFORCE),
                allow(), failure(), 500, 0),
            rollout("override ENFORCE", override(ProjectAuthorizationRolloutMode.LEGACY,
                ProjectAuthorizationRolloutMode.ENFORCE), deny(), allow(), 200, 1),
            rollout("override LEGACY", override(ProjectAuthorizationRolloutMode.ENFORCE,
                ProjectAuthorizationRolloutMode.LEGACY), allow(), deny(), 200, 1)
        );
    }

    private static Arguments rollout(
        String name,
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target,
        int expectedStatus,
        int expectedExecutions
    ) {
        return Arguments.of(new RolloutCase(
            name, configuration, legacy, target, expectedStatus, expectedExecutions));
    }

    private static ProjectAuthorizationRolloutConfiguration mode(ProjectAuthorizationRolloutMode mode) {
        return new ProjectAuthorizationRolloutConfiguration(mode, Map.of());
    }

    private static ProjectAuthorizationRolloutConfiguration override(
        ProjectAuthorizationRolloutMode defaultMode,
        ProjectAuthorizationRolloutMode overrideMode
    ) {
        return new ProjectAuthorizationRolloutConfiguration(
            defaultMode, Map.of(ProjectPolicyAction.PROJECT_SUBMIT, overrideMode));
    }

    private static ProjectAuthorizationDecision allow() {
        return ProjectAuthorizationDecision.allowed();
    }

    private static ProjectAuthorizationDecision deny() {
        return ProjectAuthorizationDecision.denied();
    }

    private static ProjectAuthorizationEvaluationFailure failure() {
        return new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED,
            EVALUATED_AT
        );
    }

    private static ProjectPolicySubjectSnapshot projectSubject() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID), EVALUATED_AT, List.of(), List.of(), Map.of());
    }

    private ProjectPolicyAuthorizationService policyService(RolloutCase rolloutCase) {
        ProjectPolicySubjectSnapshotLoader snapshotLoader =
            org.mockito.Mockito.mock(ProjectPolicySubjectSnapshotLoader.class);
        given(snapshotLoader.load(SUBJECT)).willReturn(projectSubject());
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            request -> rolloutCase.legacy(),
            request -> rolloutCase.target(),
            new ProjectAuthorizationClassifier((request, legacy, target) -> Optional.empty()),
            new ConfiguredProjectAuthorizationRolloutModeResolver(rolloutCase.configuration())
        );
        return new ProjectPolicyAuthorizationService(
            snapshotLoader,
            coordinator,
            new ProjectPolicyBundleLoader(new PolicySemanticCompiler())
        );
    }

    private SemanticAuthorization semanticAuthorization(ProjectPolicyAuthorizationService policyService) {
        LoadProjectPort loadProjectPort = org.mockito.Mockito.mock(LoadProjectPort.class);
        Project project = org.mockito.Mockito.mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getGisuId()).willReturn(1L);
        given(project.getChapterId()).willReturn(10L);
        given(project.getStatus()).willReturn(ProjectStatus.DRAFT);
        given(project.getCreatorMemberId()).willReturn(MEMBER_ID);
        given(project.getProductOwnerMemberId()).willReturn(MEMBER_ID);
        given(loadProjectPort.findById(PROJECT_ID)).willReturn(Optional.of(project));
        ProjectPermissionEvaluator evaluator = new ProjectPermissionEvaluator(
            loadProjectPort, policyService, new SuperAdminProperties(false));
        AuthorizationService authorizationService = new AuthorizationService(
            org.mockito.Mockito.mock(GetChallengerRoleUseCase.class),
            List.of(evaluator),
            org.mockito.Mockito.mock(
                com.umc.product.member.application.port.in.query.GetMemberUseCase.class),
            org.mockito.Mockito.mock(
                com.umc.product.organization.application.port.in.query.GetChapterUseCase.class),
            org.mockito.Mockito.mock(
                com.umc.product.challenger.application.port.in.query.GetChallengerUseCase.class),
            org.mockito.Mockito.mock(
                com.umc.product.organization.application.port.in.query.GetGisuUseCase.class),
            org.mockito.Mockito.mock(OperationalMetrics.class),
            Clock.fixed(EVALUATED_AT, ZoneOffset.UTC)
        );
        return new SemanticAuthorization(authorizationService, loadProjectPort);
    }

    private record RolloutCase(
        String name,
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationEvaluationResult legacy,
        ProjectAuthorizationEvaluationResult target,
        int expectedStatus,
        int expectedExecutions
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record SemanticAuthorization(
        AuthorizationService authorizationService,
        LoadProjectPort loadProjectPort
    ) {
    }
}
