package com.umc.product.project.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.authorization.adapter.in.aspect.AccessControlAspect;
import com.umc.product.authorization.adapter.in.aspect.AuthorizationRequestSubjectContext;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.assembler.ProjectApplicationResponseAssembler;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

@WebMvcTest(controllers = ProjectApplicationQueryController.class)
@Import({
    AccessControlAspect.class,
    AuthorizationRequestSubjectContext.class,
    JacksonConfig.class,
    ProjectApplicationAccessScopeResolver.class,
    ProjectApplicationDetailSubjectReuseTest.AspectProxyTestConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class ProjectApplicationDetailSubjectReuseTest {

    private static final long MEMBER_ID = 99L;
    private static final long PROJECT_ID = 42L;
    private static final long APPLICATION_ID = 500L;
    private static final Instant OUTER_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant INNER_AT = Instant.parse("2026-07-13T00:00:01Z");

    @Autowired MockMvc mockMvc;
    @Autowired ProjectApplicationAccessScopeResolver scopeResolver;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CheckPermissionUseCase checkPermissionUseCase;
    @MockitoBean ProjectApplicationResponseAssembler assembler;
    @MockitoBean ProjectPolicyAuthorizationService policyAuthorizationService;
    @MockitoBean LoadProjectMemberPort loadProjectMemberPort;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("REST 지원서 상세는 outer READ와 inner LIST_PROJECT에 같은 subject 시점을 사용한다")
    void detailReusesOneSubjectForOuterAndInnerPolicyEvaluation() throws Exception {
        SubjectAttributes outerSubject = subjectAt(OUTER_AT);
        ProjectPolicySubjectSnapshot outerSnapshot = snapshotAt(OUTER_AT);
        Project project = project();

        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(outerSubject);
        given(checkPermissionUseCase.check(eq(outerSubject), any(), eq(ProjectPolicyAction.APPLICATION_READ.id())))
            .willReturn(true);
        given(policyAuthorizationService.snapshot(outerSubject)).willReturn(outerSnapshot);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(policyAuthorizationService.evaluate(
            any(ProjectPolicySubjectSnapshot.class),
            eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT),
            any(ProjectPolicyResourceContext.class)))
            .willReturn(allowProject());
        given(assembler.detailFor(any(GetProjectApplicationDetailQuery.class), eq(outerSubject)))
            .willAnswer(invocation -> {
                scopeResolver.resolveForProjectApplicantList(outerSubject, project);
                return null;
            });

        mockMvc.perform(get(
                "/api/v1/projects/{projectId}/applications/{applicationId}", PROJECT_ID, APPLICATION_ID))
            .andExpect(status().isOk());

        verify(checkPermissionUseCase, times(1)).loadSubject(MEMBER_ID);
        verify(policyAuthorizationService, times(1)).snapshot(outerSubject);
        verify(policyAuthorizationService, never()).snapshot(MEMBER_ID);
        ArgumentCaptor<ProjectPolicySubjectSnapshot> snapshotCaptor =
            ArgumentCaptor.forClass(ProjectPolicySubjectSnapshot.class);
        verify(policyAuthorizationService).evaluate(
            snapshotCaptor.capture(), eq(ProjectPolicyAction.APPLICATION_LIST_PROJECT), any());
        org.assertj.core.api.Assertions.assertThat(snapshotCaptor.getValue().evaluatedAt()).isEqualTo(OUTER_AT);
    }

    private static SubjectAttributes subjectAt(Instant evaluatedAt) {
        return new SubjectPolicyFacts(evaluatedAt, List.of(), List.of(), Map.of())
            .toSubjectAttributes(MEMBER_ID, 1L);
    }

    private static ProjectPolicySubjectSnapshot snapshotAt(Instant evaluatedAt) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID), evaluatedAt, List.of(), List.of(), Map.of());
    }

    private static Project project() {
        Project project = Project.createDraft(10L, 20L, 77L, 30L, 77L);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    private static PolicyDecision allowProject() {
        return new PolicyDecision(
            PolicyEffect.ALLOW,
            List.of("application.list-project.super-admin"),
            List.of(),
            List.of(new PolicyResolvedOutcome(
                ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
                new PolicyValue.LongSetValue(Set.of(PROJECT_ID)))),
            INNER_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "a".repeat(64)
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectProxyTestConfiguration {
    }
}
