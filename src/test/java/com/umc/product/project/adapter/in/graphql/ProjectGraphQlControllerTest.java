package com.umc.product.project.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeGraphQlController;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayCursor;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@GraphQlTest({ProjectGraphQlController.class, NodeGraphQlController.class})
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    CurrentMemberSecurityConfig.class,
    ProjectNodeFetcher.class
})
@DisplayName("ProjectGraphQlController")
class ProjectGraphQlControllerTest {

    private static final Long REQUESTER_ID = 999L;
    private static final Long PROJECT_ID = 42L;
    private static final Long APPLICATION_ID = 1000L;

    private static final String PROJECT_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.PROJECT, 42L);
    private static final String GISU_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.GISU, 1L);
    private static final String CHAPTER_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.CHAPTER, 7L);

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
    @DisplayName("project 단건 조회는 PROJECT READ 권한을 먼저 검사하고 전역 ID로 응답한다")
    void project_단건_조회는_PROJECT_READ_권한을_먼저_검사하고_전역_ID로_응답한다() {
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    id
                    name
                    status
                    gisuId
                    chapterId
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("project.id").entity(String.class).isEqualTo(PROJECT_GLOBAL_ID)
            .path("project.name").entity(String.class).isEqualTo("Triple")
            .path("project.status").entity(String.class).isEqualTo("IN_PROGRESS")
            .path("project.gisuId").entity(String.class).isEqualTo(GISU_GLOBAL_ID)
            .path("project.chapterId").entity(String.class).isEqualTo(CHAPTER_GLOBAL_ID);

        InOrder inOrder = inOrder(checkPermissionUseCase, getProjectUseCase);
        inOrder.verify(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));
        inOrder.verify(getProjectUseCase).getById(PROJECT_ID);
    }

    @Test
    @DisplayName("project 권한이 거부되면 프로젝트 usecase를 호출하지 않는다")
    void project_권한이_거부되면_프로젝트_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("프로젝트를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    id
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "project", CommonErrorCode.FORBIDDEN));

        then(getProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("project id가 다른 타입의 전역 ID면 BAD_REQUEST를 반환하고 usecase를 호출하지 않는다")
    void project_id가_다른_타입의_전역_ID면_BAD_REQUEST를_반환하고_usecase를_호출하지_않는다() {
        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    id
                  }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.MEMBER, PROJECT_ID))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "project", CommonErrorCode.BAD_REQUEST));

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("projects는 PROJECT READ 권한을 검사하고 필터를 raw ID로 디코딩해 Connection으로 응답한다")
    void projects는_PROJECT_READ_권한을_검사하고_필터를_raw_ID로_디코딩해_Connection으로_응답한다() {
        Page<ProjectInfo> page = new PageImpl<>(
            List.of(projectInfo(), projectInfo(43L, "Second")),
            new OffsetPageRequest(0, 20),
            2
        );
        given(searchProjectUseCase.search(any(SearchProjectQuery.class), eq(REQUESTER_ID))).willReturn(page);

        graphQlTester.document("""
                query ($filter: ProjectFilterInput!) {
                  projects(filter: $filter) {
                    edges {
                      cursor
                      node {
                        id
                        name
                      }
                    }
                    pageInfo {
                      hasNextPage
                      hasPreviousPage
                      startCursor
                      endCursor
                    }
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of(
                "gisuId", GISU_GLOBAL_ID,
                "chapterId", CHAPTER_GLOBAL_ID,
                "productOwnerSchoolIds", List.of(GlobalId.encode(GlobalIdTypes.SCHOOL, 11L)),
                "parts", List.of("WEB"),
                "partQuotaStatus", "RECRUITING",
                "statuses", List.of("IN_PROGRESS", "COMPLETED")
            ))
            .execute()
            .path("projects.edges[0].cursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(0))
            .path("projects.edges[0].node.id").entity(String.class).isEqualTo(PROJECT_GLOBAL_ID)
            .path("projects.edges[0].node.name").entity(String.class).isEqualTo("Triple")
            .path("projects.edges[1].cursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(1))
            .path("projects.edges[1].node.id").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.PROJECT, 43L))
            .path("projects.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
            .path("projects.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
            .path("projects.pageInfo.startCursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(0))
            .path("projects.pageInfo.endCursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(1))
            .path("projects.totalCount").entity(Long.class).isEqualTo(2L);

        InOrder inOrder = inOrder(checkPermissionUseCase, searchProjectUseCase);
        inOrder.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, projectTypeReadPermission());
        ArgumentCaptor<SearchProjectQuery> queryCaptor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        inOrder.verify(searchProjectUseCase).search(queryCaptor.capture(), eq(REQUESTER_ID));

        SearchProjectQuery query = queryCaptor.getValue();
        assertThat(query.gisuId()).isEqualTo(1L);
        assertThat(query.chapterId()).isEqualTo(7L);
        assertThat(query.productOwnerSchoolIds()).containsExactly(11L);
        assertThat(query.parts()).containsExactly(ChallengerPart.WEB);
        assertThat(query.partQuotaStatus()).isEqualTo(PartQuotaStatus.RECRUITING);
        assertThat(query.statuses()).containsExactly(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED);
        assertThat(query.pageable().getOffset()).isEqualTo(0L);
        assertThat(query.pageable().getPageSize()).isEqualTo(20);
        assertThat(query.pageable().getSort())
            .isEqualTo(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("name")));
    }

    @Test
    @DisplayName("projects는 after 커서 다음 offset부터 orderBy 정렬로 조회한다")
    void projects는_after_커서_다음_offset부터_orderBy_정렬로_조회한다() {
        Page<ProjectInfo> page = new PageImpl<>(
            List.of(projectInfo(), projectInfo(43L, "Second")),
            new OffsetPageRequest(5, 2),
            10
        );
        given(searchProjectUseCase.search(any(SearchProjectQuery.class), eq(REQUESTER_ID))).willReturn(page);

        graphQlTester.document("""
                query ($filter: ProjectFilterInput!, $orderBy: [ProjectSort!], $first: Int, $after: String) {
                  projects(filter: $filter, orderBy: $orderBy, first: $first, after: $after) {
                    edges {
                      cursor
                    }
                    pageInfo {
                      hasNextPage
                      hasPreviousPage
                      startCursor
                      endCursor
                    }
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of("gisuId", GISU_GLOBAL_ID))
            .variable("orderBy", List.of("NAME_DESC"))
            .variable("first", 2)
            .variable("after", RelayCursor.encodeOffset(4))
            .execute()
            .path("projects.edges[0].cursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(5))
            .path("projects.edges[1].cursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(6))
            .path("projects.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
            .path("projects.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
            .path("projects.pageInfo.startCursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(5))
            .path("projects.pageInfo.endCursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(6))
            .path("projects.totalCount").entity(Long.class).isEqualTo(10L);

        ArgumentCaptor<SearchProjectQuery> queryCaptor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        then(searchProjectUseCase).should().search(queryCaptor.capture(), eq(REQUESTER_ID));

        SearchProjectQuery query = queryCaptor.getValue();
        assertThat(query.gisuId()).isEqualTo(1L);
        assertThat(query.statuses()).containsExactly(ProjectStatus.IN_PROGRESS);
        assertThat(query.pageable().getOffset()).isEqualTo(5L);
        assertThat(query.pageable().getPageSize()).isEqualTo(2);
        assertThat(query.pageable().getSort()).isEqualTo(Sort.by(Sort.Order.desc("name")));
    }

    @Test
    @DisplayName("projects first가 100을 넘으면 BAD_REQUEST GraphQL error를 반환한다")
    void projects_first가_100을_넘으면_BAD_REQUEST_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query ($filter: ProjectFilterInput!) {
                  projects(filter: $filter, first: 101) {
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of("gisuId", GISU_GLOBAL_ID))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "projects", CommonErrorCode.BAD_REQUEST));

        then(searchProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("projects last가 100을 넘으면 BAD_REQUEST GraphQL error를 반환한다")
    void projects_last가_100을_넘으면_BAD_REQUEST_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query ($filter: ProjectFilterInput!) {
                  projects(filter: $filter, last: 101) {
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of("gisuId", GISU_GLOBAL_ID))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "projects", CommonErrorCode.BAD_REQUEST));

        then(searchProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("projects 권한이 거부되면 검색 usecase를 호출하지 않는다")
    void projects_권한이_거부되면_검색_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("프로젝트 목록을 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectTypeReadPermission());

        graphQlTester.document("""
                query ($filter: ProjectFilterInput!) {
                  projects(filter: $filter) {
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of("gisuId", GISU_GLOBAL_ID))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "projects", CommonErrorCode.FORBIDDEN));

        then(searchProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Project.members는 nested field에서도 PROJECT READ 권한을 검사한다")
    void Project_members는_nested_field에서도_PROJECT_READ_권한을_검사한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    members {
                      projectMemberId
                      part
                      leader
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("project.members[0].projectMemberId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.PROJECT_MEMBER, 10L))
            .path("project.members[0].part").entity(String.class).isEqualTo("WEB")
            .path("project.members[0].leader").entity(Boolean.class).isEqualTo(false);

        then(checkPermissionUseCase).should().loadSubject(REQUESTER_ID);
        then(checkPermissionUseCase).should().check(subject, projectReadPermission(PROJECT_ID));
        then(getProjectMemberUseCase).should().listByProjectIds(List.of(PROJECT_ID));
        then(getProjectApplicationFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ProjectMember는 raw projectId 필드를 스키마에 노출하지 않는다")
    void ProjectMember는_raw_projectId_필드를_스키마에_노출하지_않는다() {
        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    members {
                      projectId
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).isNotEmpty();
                assertThat(errors.get(0).getMessage()).contains("projectId");
            });

        then(getProjectUseCase).shouldHaveNoInteractions();
        then(getProjectMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Project.applicationForm은 PROJECT READ 권한을 검사하고 질문과 옵션을 전역 ID로 응답한다")
    void Project_applicationForm은_PROJECT_READ_권한을_검사하고_질문과_옵션을_전역_ID로_응답한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(getProjectApplicationFormUseCase.findAllByProjectIds(List.of(PROJECT_ID), REQUESTER_ID))
            .willReturn(Map.of(PROJECT_ID, applicationFormInfo()));

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    applicationForm {
                      projectId
                      applicationFormId
                      title
                      sections {
                        sectionId
                        type
                        questions {
                          questionId
                          type
                          required
                          options {
                            optionId
                            content
                            other
                          }
                        }
                      }
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("project.applicationForm.projectId").entity(String.class).isEqualTo(PROJECT_GLOBAL_ID)
            .path("project.applicationForm.applicationFormId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.FORM, 500L))
            .path("project.applicationForm.sections[0].sectionId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.FORM_SECTION, 600L))
            .path("project.applicationForm.sections[0].type").entity(String.class).isEqualTo("COMMON")
            .path("project.applicationForm.sections[0].questions[0].questionId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.FORM_QUESTION, 700L))
            .path("project.applicationForm.sections[0].questions[0].required").entity(Boolean.class).isEqualTo(true)
            .path("project.applicationForm.sections[0].questions[0].options[0].optionId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.FORM_OPTION, 800L))
            .path("project.applicationForm.sections[0].questions[0].options[0].other").entity(Boolean.class)
            .isEqualTo(false);

        then(checkPermissionUseCase).should().check(subject, projectReadPermission(PROJECT_ID));
        then(getProjectApplicationFormUseCase).should().findAllByProjectIds(List.of(PROJECT_ID), REQUESTER_ID);
    }

    @Test
    @DisplayName("ProjectMember.application 권한이 없으면 지원서 상세를 조회하지 않고 null로 반환한다")
    void ProjectMember_application_권한이_없으면_지원서_상세를_조회하지_않고_null로_반환한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(checkPermissionUseCase.check(subject, applicationReadPermission(APPLICATION_ID))).willReturn(false);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    members {
                      application {
                        applicationId
                      }
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("project.members[0].application").valueIsNull();

        then(getProjectApplicationDetailUseCase).should(never()).batchGetDetails(any());
    }

    @Test
    @DisplayName("ProjectMember.application 권한이 있으면 지원서 상세를 전역 ID로 응답한다")
    void ProjectMember_application_권한이_있으면_지원서_상세를_전역_ID로_응답한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(checkPermissionUseCase.check(subject, applicationReadPermission(APPLICATION_ID))).willReturn(true);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));
        given(getProjectApplicationDetailUseCase.batchGetDetails(any()))
            .willReturn(Map.of(APPLICATION_ID, applicationDetailInfo()));

        graphQlTester.document("""
                query ($id: ID!) {
                  project(id: $id) {
                    members {
                      application {
                        applicationId
                        applicantPart
                        status
                        applicant {
                          memberId
                          part
                        }
                        matchingRound {
                          matchingRoundId
                          type
                          phase
                        }
                      }
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("project.members[0].application.applicationId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.PROJECT_APPLICATION, APPLICATION_ID))
            .path("project.members[0].application.applicantPart").entity(String.class).isEqualTo("WEB")
            .path("project.members[0].application.status").entity(String.class).isEqualTo("SUBMITTED")
            .path("project.members[0].application.applicant.memberId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.MEMBER, 200L))
            .path("project.members[0].application.applicant.part").entity(String.class).isEqualTo("WEB")
            .path("project.members[0].application.matchingRound.matchingRoundId").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.MATCHING_ROUND, 300L))
            .path("project.members[0].application.matchingRound.type").entity(String.class)
            .isEqualTo("PLAN_DEVELOPER")
            .path("project.members[0].application.matchingRound.phase").entity(String.class).isEqualTo("FIRST");

        ArgumentCaptor<Collection<GetProjectApplicationDetailQuery>> queriesCaptor = ArgumentCaptor.captor();
        then(getProjectApplicationDetailUseCase).should().batchGetDetails(queriesCaptor.capture());
        assertThat(queriesCaptor.getValue()).containsExactly(applicationDetailQuery());
    }

    @Test
    @DisplayName("node로 Project를 재조회하면 단건 조회와 동일한 권한을 검사하고 Project 타입으로 응답한다")
    void node로_Project를_재조회하면_단건_조회와_동일한_권한을_검사하고_Project_타입으로_응답한다() {
        given(getProjectUseCase.findAllByIds(List.of(PROJECT_ID)))
            .willReturn(Map.of(PROJECT_ID, projectInfo()));

        graphQlTester.document("""
                query ($id: ID!) {
                  node(id: $id) {
                    id
                    ... on Project {
                      name
                      status
                    }
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("node.id").entity(String.class).isEqualTo(PROJECT_GLOBAL_ID)
            .path("node.name").entity(String.class).isEqualTo("Triple")
            .path("node.status").entity(String.class).isEqualTo("IN_PROGRESS");

        InOrder inOrder = inOrder(checkPermissionUseCase, getProjectUseCase);
        inOrder.verify(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));
        inOrder.verify(getProjectUseCase).findAllByIds(List.of(PROJECT_ID));
    }

    @Test
    @DisplayName("node는 대상 프로젝트가 없으면 null을 반환한다")
    void node는_대상_프로젝트가_없으면_null을_반환한다() {
        given(getProjectUseCase.findAllByIds(List.of(PROJECT_ID))).willReturn(Map.of());

        graphQlTester.document("""
                query ($id: ID!) {
                  node(id: $id) {
                    id
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .path("node").valueIsNull();

        then(getProjectUseCase).should().findAllByIds(List.of(PROJECT_ID));
    }

    @Test
    @DisplayName("node 권한이 거부되면 프로젝트 usecase를 호출하지 않는다")
    void node_권한이_거부되면_프로젝트_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("프로젝트를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));

        graphQlTester.document("""
                query ($id: ID!) {
                  node(id: $id) {
                    id
                  }
                }
                """)
            .variable("id", PROJECT_GLOBAL_ID)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "node", CommonErrorCode.FORBIDDEN));

        then(getProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("node는 등록된 NodeFetcher가 없는 타입이면 null을 반환한다")
    void node는_등록된_NodeFetcher가_없는_타입이면_null을_반환한다() {
        graphQlTester.document("""
                query ($id: ID!) {
                  node(id: $id) {
                    id
                  }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.MEMBER, 200L))
            .execute()
            .path("node").valueIsNull();

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getProjectUseCase).shouldHaveNoInteractions();
    }

    private void assertCommonError(
        List<org.springframework.graphql.ResponseError> errors,
        String path,
        CommonErrorCode code
    ) {
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getPath()).isEqualTo(path);
            assertThat(error.getExtensions()).containsEntry("code", code.getCode());
        });
    }

    private ProjectInfo projectInfo() {
        return projectInfo(PROJECT_ID, "Triple");
    }

    private ProjectInfo projectInfo(Long projectId, String name) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(ProjectStatus.IN_PROGRESS)
            .name(name)
            .description("프로젝트 설명")
            .thumbnailImageUrl("https://cdn.example.com/thumb.png")
            .logoImageUrl("https://cdn.example.com/logo.png")
            .externalLink("https://example.com")
            .gisuId(1L)
            .chapterId(7L)
            .productOwnerMemberId(100L)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of(ProjectPartQuotaInfo.of(ChallengerPart.WEB, 3L, 1L)))
            .createdAt(Instant.parse("2026-06-01T00:00:00Z"))
            .updatedAt(Instant.parse("2026-06-02T00:00:00Z"))
            .build();
    }

    private ProjectMemberInfo projectMemberInfo(Long applicationId) {
        return ProjectMemberInfo.builder()
            .projectMemberId(10L)
            .projectId(PROJECT_ID)
            .applicationId(applicationId)
            .memberId(200L)
            .part(ChallengerPart.WEB)
            .isLeader(false)
            .description("Backend")
            .decidedAt(Instant.parse("2026-06-03T00:00:00Z"))
            .status(ProjectMemberStatus.ACTIVE)
            .build();
    }

    private ProjectApplicationDetailInfo applicationDetailInfo() {
        return ProjectApplicationDetailInfo.builder()
            .applicationId(APPLICATION_ID)
            .applicantMemberId(200L)
            .applicantPart(ChallengerPart.WEB)
            .matchingRoundId(300L)
            .matchingRoundType(MatchingType.PLAN_DEVELOPER)
            .matchingRoundPhase(MatchingPhase.FIRST)
            .status(ProjectApplicationViewStatus.SUBMITTED)
            .submittedAt(Instant.parse("2026-06-04T00:00:00Z"))
            .statusChangedAt(Instant.parse("2026-06-05T00:00:00Z"))
            .build();
    }

    private ApplicationFormInfo applicationFormInfo() {
        return ApplicationFormInfo.builder()
            .projectId(PROJECT_ID)
            .applicationFormId(500L)
            .title("Triple 지원서")
            .description("지원서 설명")
            .sections(List.of(ApplicationFormInfo.SectionInfo.builder()
                .sectionId(600L)
                .type(FormSectionType.COMMON)
                .allowedParts(Set.of())
                .title("공통")
                .description(null)
                .orderNo(1L)
                .questions(List.of(ApplicationFormInfo.QuestionInfo.builder()
                    .questionId(700L)
                    .type(QuestionType.RADIO)
                    .title("선호")
                    .description(null)
                    .isRequired(true)
                    .orderNo(1L)
                    .options(List.of(ApplicationFormInfo.OptionInfo.builder()
                        .optionId(800L)
                        .content("Spring")
                        .orderNo(1L)
                        .isOther(false)
                        .build()))
                    .build()))
                .build()))
            .build();
    }

    private GetProjectApplicationDetailQuery applicationDetailQuery() {
        return GetProjectApplicationDetailQuery.builder()
            .projectId(PROJECT_ID)
            .applicationId(APPLICATION_ID)
            .requesterMemberId(REQUESTER_ID)
            .build();
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .memberId(REQUESTER_ID)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .build();
    }

    private ResourcePermission projectReadPermission(Long projectId) {
        return ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);
    }

    private ResourcePermission projectTypeReadPermission() {
        return ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.READ);
    }

    private ResourcePermission applicationReadPermission(Long applicationId) {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, applicationId, PermissionType.READ);
    }
}
