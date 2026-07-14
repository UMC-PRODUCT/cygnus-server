package com.umc.product.project.adapter.in.graphql;

import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.EVALUATED_AT;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.PROJECT_ID;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.REQUESTER_ID;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.applicationPermission;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.assertForbidden;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.memberInfo;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.projectInfo;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.projectMemberInfo;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.projectMembersQuery;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.projectPermission;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.rootProjectQuery;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.subject;
import static com.umc.product.project.adapter.in.graphql.ProjectGraphQlAuthorizationFixtures.transitiveResolversQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;

@GraphQlTest(ProjectGraphQlController.class)
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class})
@DisplayName("Project GraphQL 권한 계약")
class ProjectGraphQlAuthorizationContractTest {

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetProjectUseCase getProjectUseCase;

    @MockitoBean
    SearchProjectUseCase searchProjectUseCase;

    @MockitoBean
    GetProjectMemberUseCase getProjectMemberUseCase;

    @MockitoBean
    GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;

    @MockitoBean
    GetProjectApplicationDetailUseCase getProjectApplicationDetailUseCase;

    @MockitoBean
    GetMemberUseCase getMemberUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("root project 권한이 거부되면 GraphQL error이고 하위 회원 resolver는 실행하지 않는다")
    void root_project_권한이_거부되면_GraphQL_error이고_하위_회원_resolver는_실행하지_않는다() {
        SubjectAttributes subject = subject(EVALUATED_AT);
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        willThrow(new AccessDeniedException("프로젝트를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectPermission(), ProjectPolicyAction.PROJECT_READ.id());
        willThrow(new AccessDeniedException("프로젝트를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(
                same(subject),
                eq(projectPermission()),
                eq(ProjectPolicyAction.PROJECT_READ.id())
            );

        graphQlTester.document(rootProjectQuery())
            .execute()
            .errors()
            .satisfy(errors -> assertForbidden(errors, "project"));

        then(getProjectUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Project.members 권한이 거부되면 ProjectMember.member를 노출하지 않는다")
    void Project_members_권한이_거부되면_ProjectMember_member를_노출하지_않는다() {
        SubjectAttributes subject = subject(EVALUATED_AT);
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.check(
            subject,
            projectPermission(),
            ProjectPolicyAction.PROJECT_MEMBER_LIST.id()
        )).willReturn(false);

        graphQlTester.document(projectMembersQuery())
            .execute()
            .errors()
            .satisfy(errors -> assertForbidden(errors, null));

        then(getProjectMemberUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("parent 권한이 있는 세 transitive resolver와 application 평가는 동일 snapshot을 재사용한다")
    void parent_권한이_있는_세_transitive_resolver와_application_평가는_동일_snapshot을_재사용한다() {
        SubjectAttributes subject = subject(EVALUATED_AT);
        SubjectAttributes changedSubject = subject(EVALUATED_AT.plusSeconds(1));
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject, changedSubject);
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.check(
            subject,
            projectPermission(),
            ProjectPolicyAction.PROJECT_MEMBER_LIST.id()
        )).willReturn(true);
        given(checkPermissionUseCase.check(
            subject,
            applicationPermission(),
            ProjectPolicyAction.APPLICATION_READ.id()
        )).willReturn(false);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID)))
            .willReturn(Map.of(PROJECT_ID, List.of(projectMemberInfo())));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of(
            100L, memberInfo(100L, "po"),
            101L, memberInfo(101L, "co-po"),
            200L, memberInfo(200L, "member")
        ));

        graphQlTester.document(transitiveResolversQuery())
            .execute()
            .errors().verify()
            .path("project.productOwner.memberId").entity(String.class).isEqualTo("100")
            .path("project.coProductOwners[0].memberId").entity(String.class).isEqualTo("101")
            .path("project.members[0].member.memberId").entity(String.class).isEqualTo("200")
            .path("project.members[0].application").valueIsNull();

        assertThat(subject.policyFacts().evaluatedAt()).isEqualTo(EVALUATED_AT);
        then(checkPermissionUseCase).should(times(1)).loadSubject(REQUESTER_ID);
        then(checkPermissionUseCase).should().checkOrThrow(
            same(subject),
            eq(projectPermission()),
            eq(ProjectPolicyAction.PROJECT_READ.id())
        );
        then(checkPermissionUseCase).should().check(
            same(subject),
            eq(projectPermission()),
            eq(ProjectPolicyAction.PROJECT_MEMBER_LIST.id())
        );
        then(checkPermissionUseCase).should().check(
            same(subject),
            eq(applicationPermission()),
            eq(ProjectPolicyAction.APPLICATION_READ.id())
        );
        then(getProjectApplicationDetailUseCase).should(never()).batchGetDetails(any(), any());
        then(getProjectApplicationDetailUseCase).should(never()).batchGetDetails(any());
    }

}
